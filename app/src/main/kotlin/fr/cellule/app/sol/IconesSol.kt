package fr.cellule.app.sol

import androidx.compose.ui.graphics.vector.ImageVector
import fr.cellule.app.IconeProjecteur
import fr.cellule.app.Onglet
import fr.cellule.app.icone
import fr.cellule.core.TypeElement

/*
 * Les icônes de l'éditeur du plan au sol : outils, actions et types
 * d'éléments, au même trait que les onglets (24 × 24, trait 1,7).
 */

internal val IconeAjouterSol = icone("ajouter") {
    moveTo(12f, 5f); lineTo(12f, 19f)
    moveTo(5f, 12f); lineTo(19f, 12f)
}

internal val IconeMoinsSol = icone("moins") {
    moveTo(5f, 12f); lineTo(19f, 12f)
}

/** Quatre coins : faire tenir tout le plan. */
internal val IconeToutVoirSol = icone("tout-voir") {
    moveTo(4f, 9f); lineTo(4f, 4f); lineTo(9f, 4f)
    moveTo(15f, 4f); lineTo(20f, 4f); lineTo(20f, 9f)
    moveTo(20f, 15f); lineTo(20f, 20f); lineTo(15f, 20f)
    moveTo(9f, 20f); lineTo(4f, 20f); lineTo(4f, 15f)
}

/** Une règle posée en biais, et ses graduations. */
internal val IconeRegleSol = icone("regle") {
    moveTo(3f, 15f); lineTo(15f, 3f); lineTo(21f, 9f); lineTo(9f, 21f); close()
    moveTo(6f, 12f); lineTo(7.6f, 13.6f)
    moveTo(9f, 9f); lineTo(11.2f, 11.2f)
    moveTo(12f, 6f); lineTo(13.6f, 7.6f)
}

/** Une flèche qui sort d'un bac : exporter. */
internal val IconeExporterSol = icone("exporter") {
    moveTo(5f, 14f); lineTo(5f, 19f); lineTo(19f, 19f); lineTo(19f, 14f)
    moveTo(12f, 15f); lineTo(12f, 4f)
    moveTo(8f, 8f); lineTo(12f, 4f); lineTo(16f, 8f)
}

internal val IconeAnnulerSol = icone("annuler") {
    moveTo(5f, 10f); lineTo(14.5f, 10f)
    arcTo(4.5f, 4.5f, 0f, false, true, 14.5f, 19f)
    lineTo(10f, 19f)
    moveTo(8.5f, 6.5f); lineTo(5f, 10f); lineTo(8.5f, 13.5f)
}

internal val IconeRetablirSol = icone("retablir") {
    moveTo(19f, 10f); lineTo(9.5f, 10f)
    arcTo(4.5f, 4.5f, 0f, false, false, 9.5f, 19f)
    lineTo(14f, 19f)
    moveTo(15.5f, 6.5f); lineTo(19f, 10f); lineTo(15.5f, 13.5f)
}

/** Trois points : le menu. */
internal val IconeMenuSol = icone("menu") {
    moveTo(3.8f, 12f); arcTo(1.2f, 1.2f, 0f, true, true, 6.2f, 12f); arcTo(1.2f, 1.2f, 0f, true, true, 3.8f, 12f); close()
    moveTo(10.8f, 12f); arcTo(1.2f, 1.2f, 0f, true, true, 13.2f, 12f); arcTo(1.2f, 1.2f, 0f, true, true, 10.8f, 12f); close()
    moveTo(17.8f, 12f); arcTo(1.2f, 1.2f, 0f, true, true, 20.2f, 12f); arcTo(1.2f, 1.2f, 0f, true, true, 17.8f, 12f); close()
}

internal val IconeDupliquerSol = icone("dupliquer") {
    moveTo(8f, 8f); lineTo(19f, 8f); lineTo(19f, 19f); lineTo(8f, 19f); close()
    moveTo(5f, 15f); lineTo(5f, 5f); lineTo(15f, 5f)
}

/** Une flèche : la position suivante d'une caméra ou d'un comédien. */
internal val IconeSuivanteSol = icone("suivante") {
    moveTo(4f, 12f); lineTo(18f, 12f)
    moveTo(13f, 7f); lineTo(18f, 12f); lineTo(13f, 17f)
}

/** Une calculatrice : déplacer le projecteur par le calcul. */
internal val IconeCalculerSol = icone("calculer") {
    moveTo(6f, 3f); lineTo(18f, 3f); lineTo(18f, 21f); lineTo(6f, 21f); close()
    moveTo(9f, 7f); lineTo(15f, 7f)
    moveTo(9f, 12f); lineTo(11f, 12f); moveTo(10f, 11f); lineTo(10f, 13f)
    moveTo(13f, 12f); lineTo(15f, 12f)
    moveTo(9f, 16.5f); lineTo(11f, 16.5f)
    moveTo(13f, 15.5f); lineTo(15f, 15.5f); moveTo(13f, 17.5f); lineTo(15f, 17.5f)
}

internal val IconeSupprimerSol = icone("supprimer") {
    moveTo(5f, 7f); lineTo(19f, 7f)
    moveTo(10f, 7f); lineTo(10f, 4.5f); lineTo(14f, 4.5f); lineTo(14f, 7f)
    moveTo(7f, 7f); lineTo(8f, 20f); lineTo(16f, 20f); lineTo(17f, 7f)
    moveTo(10.5f, 10.5f); lineTo(10.5f, 16.5f)
    moveTo(13.5f, 10.5f); lineTo(13.5f, 16.5f)
}

private val IconeComedien = icone("comedien") {
    moveTo(9f, 7.5f); arcTo(3f, 3f, 0f, true, true, 15f, 7.5f); arcTo(3f, 3f, 0f, true, true, 9f, 7.5f); close()
    moveTo(5f, 20f); arcTo(7f, 7f, 0f, false, true, 19f, 20f)
}

private val IconeRail = icone("rail") {
    moveTo(3f, 9f); lineTo(21f, 9f)
    moveTo(3f, 15f); lineTo(21f, 15f)
    moveTo(6f, 7f); lineTo(6f, 17f)
    moveTo(12f, 7f); lineTo(12f, 17f)
    moveTo(18f, 7f); lineTo(18f, 17f)
}

private val IconeReflecteur = icone("reflecteur") {
    moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(4f, 19f); close()
    moveTo(4f, 13f); lineTo(12f, 5f)
    moveTo(8f, 19f); lineTo(20f, 7f)
    moveTo(14f, 19f); lineTo(20f, 13f)
}

private val IconeDiffusion = icone("diffusion") {
    moveTo(3f, 4f); lineTo(21f, 4f); lineTo(21f, 20f); lineTo(3f, 20f); close()
    moveTo(6f, 7f); lineTo(18f, 7f); lineTo(18f, 17f); lineTo(6f, 17f); close()
}

private val IconeDrapeau = icone("drapeau") {
    moveTo(6f, 3f); lineTo(6f, 21f)
    moveTo(6f, 4f); lineTo(18f, 4f); lineTo(18f, 13f); lineTo(6f, 13f)
    moveTo(6f, 8.5f); lineTo(18f, 8.5f)
}

private val IconeMur = icone("mur") {
    moveTo(3f, 9f); lineTo(21f, 9f); lineTo(21f, 15f); lineTo(3f, 15f); close()
    moveTo(7f, 15f); lineTo(11f, 9f)
    moveTo(13f, 15f); lineTo(17f, 9f)
}

private val IconeFenetre = icone("fenetre") {
    moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); lineTo(4f, 20f); close()
    moveTo(12f, 4f); lineTo(12f, 20f)
    moveTo(4f, 12f); lineTo(20f, 12f)
}

private val IconePorte = icone("porte") {
    moveTo(3f, 20f); lineTo(6f, 20f)
    moveTo(18f, 20f); lineTo(21f, 20f)
    moveTo(6f, 20f); lineTo(6f, 8f)
    arcTo(12f, 12f, 0f, false, true, 18f, 20f)
}

private val IconeMeuble = icone("meuble") {
    moveTo(5f, 8f); lineTo(19f, 8f); lineTo(19f, 16f); lineTo(5f, 16f); close()
    moveTo(9f, 4.5f); lineTo(15f, 4.5f)
    moveTo(9f, 19.5f); lineTo(15f, 19.5f)
}

private val IconeRepere = icone("repere") {
    moveTo(6f, 12f); arcTo(6f, 6f, 0f, true, true, 18f, 12f); arcTo(6f, 6f, 0f, true, true, 6f, 12f); close()
    moveTo(12f, 3f); lineTo(12f, 21f)
    moveTo(3f, 12f); lineTo(21f, 12f)
}

internal fun iconeType(t: TypeElement): ImageVector = when (t) {
    TypeElement.CAMERA -> Onglet.CAMERAS.icone
    TypeElement.COMEDIEN -> IconeComedien
    TypeElement.PROJECTEUR -> IconeProjecteur
    TypeElement.REFLECTEUR -> IconeReflecteur
    TypeElement.DIFFUSION -> IconeDiffusion
    TypeElement.DRAPEAU -> IconeDrapeau
    TypeElement.RAIL -> IconeRail
    TypeElement.MUR -> IconeMur
    TypeElement.FENETRE -> IconeFenetre
    TypeElement.PORTE -> IconePorte
    TypeElement.MEUBLE -> IconeMeuble
    TypeElement.REPERE -> IconeRepere
}

/** Son nom court, pour la grille « Ajouter ». */
internal fun nomCourt(t: TypeElement): String = when (t) {
    TypeElement.DIFFUSION -> "Diffusion"
    TypeElement.RAIL -> "Rail"
    else -> t.libelle
}

/** Les familles de la grille « Ajouter ». */
internal val FAMILLES_SOL: List<Pair<String, List<TypeElement>>> = listOf(
    "Personnes et caméra" to listOf(TypeElement.CAMERA, TypeElement.COMEDIEN, TypeElement.RAIL),
    "Lumière" to listOf(TypeElement.PROJECTEUR, TypeElement.REFLECTEUR, TypeElement.DIFFUSION, TypeElement.DRAPEAU),
    "Décor" to listOf(TypeElement.MUR, TypeElement.FENETRE, TypeElement.PORTE, TypeElement.MEUBLE),
    "Repères" to listOf(TypeElement.REPERE)
)
