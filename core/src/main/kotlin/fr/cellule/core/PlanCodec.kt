package fr.cellule.core

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/*
 * Un plan au sol en texte court, pour le passer d'un appareil à l'autre :
 * compressé, en Base64, et découpé en morceaux qui tiennent chacun dans un
 * QR code lisible à l'écran. Chaque morceau dit sa place : « CELLULE1:2/3: ».
 */
object PlanCodec {

    const val PREFIXE = "CELLULE1"

    /** Au-delà, un QR code affiché sur un téléphone devient difficile à lire. */
    const val TAILLE_MORCEAU = 900

    fun compresser(texte: String): String {
        val d = Deflater(Deflater.BEST_COMPRESSION)
        d.setInput(texte.toByteArray(Charsets.UTF_8))
        d.finish()
        val sortie = ByteArrayOutputStream()
        val tampon = ByteArray(4096)
        while (!d.finished()) sortie.write(tampon, 0, d.deflate(tampon))
        d.end()
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sortie.toByteArray())
    }

    fun decompresser(code: String): String {
        val octets = Base64.getUrlDecoder().decode(code)
        val i = Inflater()
        i.setInput(octets)
        val sortie = ByteArrayOutputStream()
        val tampon = ByteArray(4096)
        while (!i.finished()) {
            val n = i.inflate(tampon)
            if (n == 0 && (i.needsInput() || i.needsDictionary())) break
            sortie.write(tampon, 0, n)
        }
        i.end()
        return sortie.toString(Charsets.UTF_8.name())
    }

    /** Les morceaux à afficher en QR codes, ou à coller l'un après l'autre. */
    fun morceaux(json: String, taille: Int = TAILLE_MORCEAU): List<String> {
        val code = compresser(json)
        val parts = code.chunked(taille)
        return parts.mapIndexed { i, p -> "$PREFIXE:${i + 1}/${parts.size}:$p" }
    }

    /** Un morceau lu : sa place, le total, son contenu ; null si ce n'est pas un morceau de plan. */
    fun lire(morceau: String): Triple<Int, Int, String>? {
        val m = Regex("^$PREFIXE:(\\d+)/(\\d+):(.+)$").find(morceau.trim()) ?: return null
        val i = m.groupValues[1].toInt()
        val n = m.groupValues[2].toInt()
        if (i < 1 || n < 1 || i > n) return null
        return Triple(i, n, m.groupValues[3])
    }

    /** Le plan reconstitué quand tous les morceaux sont là ; null sinon. */
    fun assembler(morceaux: Collection<String>): String? {
        val lus = morceaux.mapNotNull { lire(it) }
        if (lus.isEmpty()) return null
        val n = lus.first().second
        val parPlace = lus.filter { it.second == n }.associateBy { it.first }
        if (parPlace.size != n) return null
        return runCatching { decompresser((1..n).joinToString("") { parPlace.getValue(it).third }) }.getOrNull()
    }

    /** Un texte collé qui contient un ou plusieurs morceaux, un par ligne ou à la suite. */
    fun depuisTexte(texte: String): String? =
        assembler(Regex("$PREFIXE:\\d+/\\d+:[A-Za-z0-9_-]+").findAll(texte).map { it.value }.toList())
}
