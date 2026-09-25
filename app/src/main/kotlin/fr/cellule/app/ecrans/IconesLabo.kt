package fr.cellule.app.ecrans

import fr.cellule.app.icone

/*
 * Les glyphes des tuiles du Labo, dans le même trait que la barre de
 * navigation : 24 × 24, trait de 1,7, bouts arrondis.
 */

/** Un thermomètre : temps et température. */
val IconeTemperature = icone("temperature") {
    moveTo(10f, 14f); lineTo(10f, 5f)
    arcTo(2f, 2f, 0f, false, true, 14f, 5f)
    lineTo(14f, 14f)
    arcTo(3.6f, 3.6f, 0f, true, true, 10f, 14f)
    moveTo(12f, 16.5f); lineTo(12f, 9f)
    moveTo(16.5f, 8f); lineTo(18f, 8f)
    moveTo(16.5f, 11f); lineTo(18f, 11f)
}

/** Deux flèches opposées : pousser, retenir. */
val IconePush = icone("push") {
    moveTo(8f, 19f); lineTo(8f, 5f)
    moveTo(5f, 8f); lineTo(8f, 5f); lineTo(11f, 8f)
    moveTo(16f, 5f); lineTo(16f, 19f)
    moveTo(13f, 16f); lineTo(16f, 19f); lineTo(19f, 16f)
}

/** Un sablier : les poses longues. */
val IconeReciprocite = icone("reciprocite") {
    moveTo(6f, 4f); lineTo(18f, 4f)
    moveTo(6f, 20f); lineTo(18f, 20f)
    moveTo(8f, 4f); lineTo(16f, 4f); lineTo(12f, 12f); close()
    moveTo(12f, 12f); lineTo(16f, 20f); lineTo(8f, 20f); close()
}

/** Une goutte : les dilutions. */
val IconeDilution = icone("dilution") {
    moveTo(12f, 3.5f); lineTo(7.4f, 12.5f)
    arcTo(4.8f, 4.8f, 0f, true, false, 16.6f, 12.5f)
    close()
}

/** Une bande d'essai : le tirage en diaphs. */
val IconeTirage = icone("tirage") {
    moveTo(4f, 6f); lineTo(20f, 6f); lineTo(20f, 18f); lineTo(4f, 18f); close()
    moveTo(8f, 6f); lineTo(8f, 18f)
    moveTo(12f, 6f); lineTo(12f, 18f)
    moveTo(16f, 6f); lineTo(16f, 18f)
}

/** Un chronomètre. */
val IconeChrono = icone("chrono") {
    moveTo(5f, 13.5f)
    arcTo(7f, 7f, 0f, true, true, 19f, 13.5f)
    arcTo(7f, 7f, 0f, true, true, 5f, 13.5f)
    close()
    moveTo(12f, 13.5f); lineTo(12f, 9.5f)
    moveTo(10f, 3f); lineTo(14f, 3f)
    moveTo(12f, 3f); lineTo(12f, 6.5f)
}

/** Une page cornée : les fiches produits. */
val IconeFiches = icone("fiches") {
    moveTo(6f, 3f); lineTo(14.5f, 3f); lineTo(18f, 6.5f); lineTo(18f, 21f); lineTo(6f, 21f); close()
    moveTo(14.5f, 3f); lineTo(14.5f, 6.5f); lineTo(18f, 6.5f)
    moveTo(9f, 11f); lineTo(15f, 11f)
    moveTo(9f, 14f); lineTo(15f, 14f)
    moveTo(9f, 17f); lineTo(13f, 17f)
}

/** Une bulle et son point d'interrogation : le quiz. */
val IconeQuiz = icone("quiz") {
    moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 16f); lineTo(11f, 16f); lineTo(7f, 19.5f); lineTo(7f, 16f); lineTo(4f, 16f); close()
    moveTo(10.2f, 8.9f)
    arcTo(1.9f, 1.9f, 0f, true, true, 12f, 11.2f)
    lineTo(12f, 12f)
    moveTo(12f, 13.9f); lineTo(12f, 14f)
}
