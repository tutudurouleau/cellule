package fr.cellule.app.ecrans

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import fr.cellule.core.Materiel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Ce que la licence d'une photo exige d'afficher à côté d'elle. */
data class CreditPhoto(
    val fichier: String,
    val auteur: String,
    val licence: String,
    val source: String,
    val plateforme: String
) {
    val mention: String get() = "Photo : $auteur · $licence · $plateforme"
}

/*
 * Les photos viennent de Wikimedia Commons ou de Flickr (trouvées par
 * Openverse), toujours sous licence libre, et sont embarquées dans l'APK
 * (dossier assets/photos) : l'application reste entièrement hors ligne.
 * Elles s'affichent dans la fiche et le quiz ; la liste garde les
 * silhouettes dessinées, comme les fiches sans photo.
 */
object Photos {

    /** Largeur de décodage, en pixels, d'une photo de fiche ou de quiz. */
    const val PLEINE = 1080

    @Volatile
    private var credits: Map<String, CreditPhoto>? = null

    fun credit(contexte: Context, m: Materiel): CreditPhoto? =
        (credits ?: lireCredits(contexte).also { credits = it })[m.identifiant]

    private fun lireCredits(contexte: Context): Map<String, CreditPhoto> = try {
        val json = contexte.assets.open("photos/credits.json").bufferedReader().use { it.readText() }
        val racine = JSONObject(json)
        racine.keys().asSequence().associateWith { cle ->
            val c = racine.getJSONObject(cle)
            CreditPhoto(
                fichier = c.getString("fichier"),
                auteur = c.optString("auteur").ifBlank { "auteur inconnu" },
                licence = c.optString("licence"),
                source = c.optString("source"),
                plateforme = c.optString("plateforme").ifBlank { "Wikimedia Commons" }
            )
        }
    } catch (e: Exception) {
        emptyMap()
    }

    /* Une photo de 960 px pèse près de 4 Mo une fois décodée : on garde les
       dernières photos ouvertes, pas tout le catalogue. */
    private val cache = object : LruCache<String, ImageBitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    fun enCache(m: Materiel, largeur: Int): ImageBitmap? = cache.get("${m.identifiant}@$largeur")

    /** Décode la photo, sous-échantillonnée si elle dépasse la largeur demandée. */
    fun charger(contexte: Context, m: Materiel, largeur: Int): ImageBitmap? {
        val cle = "${m.identifiant}@$largeur"
        cache.get(cle)?.let { return it }
        val credit = credit(contexte, m) ?: return null
        val chemin = "photos/${credit.fichier}"
        return try {
            val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contexte.assets.open(chemin).use { BitmapFactory.decodeStream(it, null, bornes) }
            var echantillon = 1
            while (bornes.outWidth / (echantillon * 2) >= largeur) echantillon *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = echantillon }
            contexte.assets.open(chemin).use { BitmapFactory.decodeStream(it, null, options) }
                ?.asImageBitmap()
                ?.also { cache.put(cle, it) }
        } catch (e: Exception) {
            null
        }
    }
}

/** La photo d'une fiche, décodée hors du fil principal ; null tant qu'elle charge ou s'il n'y en a pas. */
@Composable
fun photoDe(m: Materiel, largeur: Int): ImageBitmap? {
    val contexte = LocalContext.current.applicationContext
    val photo by produceState(Photos.enCache(m, largeur), m, largeur) {
        /* Sans cette remise à zéro, la photo de la question précédente resterait
           affichée le temps de décoder la suivante. */
        value = Photos.enCache(m, largeur)
        value = withContext(Dispatchers.IO) { Photos.charger(contexte, m, largeur) }
    }
    return photo
}
