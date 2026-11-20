package fr.cellule.core

/*
 * La demande de location : ce qu'on envoie au loueur pour obtenir un devis.
 * L'appli ne l'envoie jamais elle-même — elle fabrique un PDF propre que la
 * personne relit et transmet de son côté.
 *
 * Les loueurs ne publient pas leurs tarifs : l'estimation ne s'appuie que
 * sur « mes tarifs », les prix que la personne a relevés sur ses vrais devis.
 */

data class InfosProduction(
    val titre: String = "",
    val societe: String = "",
    val contact: String = "",
    val telephone: String = "",
    val email: String = "",
    val livraison: String = "",
    val enlevement: String = "",
    val retour: String = "",
    val joursTournage: Int = 1,
    val notes: String = ""
) {
    /** Ce qui manque pour que le loueur puisse répondre. */
    val manques: List<String>
        get() = listOfNotNull(
            "le titre du projet".takeIf { titre.isBlank() },
            "un nom de contact".takeIf { contact.isBlank() },
            "un téléphone ou un e-mail".takeIf { telephone.isBlank() && email.isBlank() },
            "les dates d'enlèvement et de retour".takeIf { enlevement.isBlank() || retour.isBlank() }
        )
}

object DemandeLocation {

    /** Les infos de production en texte simple : « clé=valeur », une par ligne. */
    fun lireInfos(texte: String): InfosProduction {
        val m = texte.lines().mapNotNull { l ->
            val i = l.indexOf('=')
            if (i <= 0) null else l.substring(0, i) to l.substring(i + 1).replace("\\n", "\n")
        }.toMap()
        return InfosProduction(
            m["titre"].orEmpty(), m["societe"].orEmpty(), m["contact"].orEmpty(), m["telephone"].orEmpty(),
            m["email"].orEmpty(), m["livraison"].orEmpty(), m["enlevement"].orEmpty(), m["retour"].orEmpty(),
            m["jours"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1, m["notes"].orEmpty()
        )
    }

    fun ecrireInfos(i: InfosProduction): String = listOf(
        "titre" to i.titre, "societe" to i.societe, "contact" to i.contact, "telephone" to i.telephone,
        "email" to i.email, "livraison" to i.livraison, "enlevement" to i.enlevement, "retour" to i.retour,
        "jours" to i.joursTournage.toString(), "notes" to i.notes
    ).joinToString("\n") { (k, v) -> k + "=" + v.replace("\n", "\\n") }

    /** Le service du loueur qui traite chaque catégorie de la liste. */
    fun departement(categorie: String): Departement? = when (categorie) {
        "Caméra" -> Departement.CAMERA
        "Projecteurs" -> Departement.LUMIERE
        "Accessoires", "Machinerie" -> Departement.MACHINERIE
        "Électrique" -> Departement.ELECTRIQUE
        else -> null
    }

    /** Les lignes, rangées par service du loueur ; les consommables à part. */
    fun parService(lignes: List<LigneLocation>): List<Pair<String, List<LigneLocation>>> {
        val groupes = lignes.groupBy { departement(it.categorie)?.libelle ?: "Consommables" }
        val ordre = Departement.entries.map { it.libelle } + "Consommables"
        return ordre.mapNotNull { o -> groupes[o]?.let { o to it.sortedWith(compareBy({ it.auto }, { it.nom })) } }
    }

    /** Ce qu'il est bon de rappeler au loueur et à soi-même en bas de la demande. */
    val MENTIONS = listOf(
        "Merci de nous adresser votre devis pour cette liste et ces dates, et de nous signaler toute référence indisponible ou remplacée.",
        "Les lignes « ajouté d'office » (pieds, sacs, ballasts, prolongs) sont une proposition : à ajuster avec vous.",
        "Caution, assurance du matériel et franchise : selon vos conditions générales de location."
    )
}

/** Les prix relevés sur les vrais devis, hors taxes et par jour : « désignation\tprix ». */
object MesTarifs {
    fun lire(texte: String): Map<String, Double> = texte.lines().mapNotNull { l ->
        val m = l.split('\t')
        val prix = m.getOrNull(1)?.replace(',', '.')?.trim()?.toDoubleOrNull()
        if (m.size < 2 || m[0].isBlank() || prix == null || prix < 0) null else m[0].trim() to prix
    }.toMap()

    fun ecrire(t: Map<String, Double>): String = t.entries.sortedBy { it.key }.joinToString("\n") { "${it.key}\t${it.value}" }

    data class Estimation(val parJour: Double, val lignesChiffrees: Int, val lignesTotal: Int) {
        val complete: Boolean get() = lignesChiffrees == lignesTotal
    }

    /** Le total par jour des lignes dont on connaît le prix ; les autres restent à demander. */
    fun estimer(lignes: List<LigneLocation>, tarifs: Map<String, Double>): Estimation {
        val chiffrees = lignes.filter { it.nom in tarifs }
        val parJour = chiffrees.sumOf { (tarifs[it.nom] ?: 0.0) * if (it.unite.isBlank()) it.quantite else 1.0 }
        return Estimation(parJour, chiffrees.size, lignes.size)
    }
}

/** La check-list chez le loueur : chaque ligne comptée à l'enlèvement et au retour. */
data class PointageLigne(val nom: String, val prevu: Int, val enleve: Int = 0, val rendu: Int = 0, val note: String = "") {
    val manqueAuDepart: Int get() = (prevu - enleve).coerceAtLeast(0)
    val manqueAuRetour: Int get() = (enleve - rendu).coerceAtLeast(0)
}

object Pointage {
    /** Le pointage d'une liste : celui déjà commencé, complété des lignes nouvelles. */
    fun pour(lignes: List<LigneLocation>, deja: List<PointageLigne>): List<PointageLigne> = lignes.map { l ->
        val prevu = if (l.unite.isBlank()) l.quantite.toInt() else 1
        deja.firstOrNull { it.nom == l.nom }?.copy(prevu = prevu) ?: PointageLigne(l.nom, prevu)
    }

    fun lire(texte: String): List<PointageLigne> = texte.lines().mapNotNull { l ->
        val m = l.split('\t')
        if (m.size < 4) null
        else PointageLigne(m[0], m[1].toIntOrNull() ?: 0, m[2].toIntOrNull() ?: 0, m[3].toIntOrNull() ?: 0, m.getOrNull(4).orEmpty())
    }

    fun ecrire(p: List<PointageLigne>): String = p.joinToString("\n") {
        listOf(it.nom, it.prevu, it.enleve, it.rendu, it.note.replace('\t', ' ').replace('\n', ' ')).joinToString("\t")
    }

    fun bilan(p: List<PointageLigne>): String {
        val depart = p.sumOf { it.manqueAuDepart }
        val retour = p.sumOf { it.manqueAuRetour }
        return when {
            p.all { it.enleve == 0 } -> "Rien de pointé pour l'instant."
            depart > 0 -> "$depart pièce(s) pas encore pointée(s) à l'enlèvement."
            p.all { it.rendu == 0 } -> "Tout est enlevé. Au retour, pointe chaque pièce rendue."
            retour > 0 -> "$retour pièce(s) pas encore rendue(s)."
            else -> "Tout est rendu."
        }
    }
}
