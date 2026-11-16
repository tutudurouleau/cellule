package fr.cellule.app.mesure

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.LruCache
import fr.cellule.core.Tuiles
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.floor

/**
 * Les tuiles d'OpenStreetMap, téléchargées à la demande et gardées en cache.
 *
 * La carte n'est qu'un confort : sans réseau, rien ne se télécharge et
 * l'appli s'en passe. On respecte la politique d'usage des tuiles d'OSM :
 * un User-Agent qui nomme l'appli, un cache sur le téléphone pour ne pas
 * redemander ce qu'on a déjà, jamais de téléchargement en masse, et la
 * mention des contributeurs affichée avec la carte.
 */
object TuilesOsm {

    /** Une tuile en cache reste valable une semaine. */
    private const val DUREE_CACHE_MS = 7L * 24 * 3600 * 1000

    private val memoire = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun agent(contexte: Context): String {
        val version = runCatching {
            contexte.packageManager.getPackageInfo(contexte.packageName, 0).versionName
        }.getOrNull() ?: "?"
        return "Cellule/$version (Android ; ${contexte.packageName})"
    }

    private fun fichier(contexte: Context, zoom: Int, colonne: Int, ligne: Int): File =
        File(File(contexte.cacheDir, "tuiles").apply { mkdirs() }, "${zoom}_${colonne}_$ligne.png")

    /** La tuile si elle est déjà en mémoire : ne touche ni au disque ni au réseau. */
    fun enMemoire(zoom: Int, colonne: Int, ligne: Int): Bitmap? = memoire.get("$zoom/$colonne/$ligne")

    /**
     * La tuile, depuis la mémoire, le cache du téléphone ou — à défaut et s'il
     * y a du réseau — le serveur. Bloquant : à appeler hors du fil principal.
     */
    fun tuile(contexte: Context, zoom: Int, colonne: Int, ligne: Int): Bitmap? {
        val cle = "$zoom/$colonne/$ligne"
        memoire.get(cle)?.let { return it }
        val cache = fichier(contexte, zoom, colonne, ligne)
        val frais = cache.exists() && System.currentTimeMillis() - cache.lastModified() < DUREE_CACHE_MS
        if (frais || (cache.exists() && !reseauDisponible(contexte))) {
            BitmapFactory.decodeFile(cache.absolutePath)?.let {
                memoire.put(cle, it)
                return it
            }
        }
        if (!reseauDisponible(contexte)) return null
        var connexion: HttpURLConnection? = null
        return try {
            connexion = URL(Tuiles.adresse(zoom, colonne, ligne)).openConnection() as HttpURLConnection
            connexion.connectTimeout = 6000
            connexion.readTimeout = 8000
            connexion.setRequestProperty("User-Agent", agent(contexte))
            if (connexion.responseCode != 200) return null
            val octets = connexion.inputStream.use { it.readBytes() }
            val image = BitmapFactory.decodeByteArray(octets, 0, octets.size) ?: return null
            runCatching { cache.writeBytes(octets) }
            memoire.put(cle, image)
            image
        } catch (e: Exception) {
            null
        } finally {
            connexion?.disconnect()
        }
    }

    /**
     * Une petite carte carrée centrée sur un point, pour un PDF : quatre
     * tuiles au plus. Null sans réseau ni cache.
     */
    fun vignette(contexte: Context, latitude: Double, longitude: Double, zoom: Int = 15, cote: Int = 384): Bitmap? {
        val cx = Tuiles.x(longitude, zoom) * Tuiles.COTE
        val cy = Tuiles.y(latitude, zoom) * Tuiles.COTE
        val gauche = cx - cote / 2.0
        val haut = cy - cote / 2.0
        val sortie = Bitmap.createBitmap(cote, cote, Bitmap.Config.ARGB_8888)
        val toile = Canvas(sortie)
        var obtenues = 0
        val premiereColonne = floor(gauche / Tuiles.COTE).toInt()
        val derniereColonne = floor((gauche + cote - 1) / Tuiles.COTE).toInt()
        val premiereLigne = floor(haut / Tuiles.COTE).toInt()
        val derniereLigne = floor((haut + cote - 1) / Tuiles.COTE).toInt()
        for (tx in premiereColonne..derniereColonne) {
            for (ty in premiereLigne..derniereLigne) {
                val ligne = Tuiles.ligne(ty.toDouble(), zoom) ?: continue
                val image = tuile(contexte, zoom, Tuiles.colonne(tx.toDouble(), zoom), ligne) ?: continue
                toile.drawBitmap(
                    image, (tx * Tuiles.COTE - gauche).toFloat(), (ty * Tuiles.COTE - haut).toFloat(), null
                )
                obtenues++
            }
        }
        if (obtenues == 0) return null
        /* Le point lui-même : un disque cerclé de blanc. */
        val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)
        pinceau.color = android.graphics.Color.WHITE
        toile.drawCircle(cote / 2f, cote / 2f, 9f, pinceau)
        pinceau.color = 0xFFD9342B.toInt()
        toile.drawCircle(cote / 2f, cote / 2f, 6.5f, pinceau)
        return sortie
    }
}
