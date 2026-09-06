package fr.cellule.core

import kotlin.math.log2

enum class Modele { PHYSIQUE, GRILLE_MENTALE }

data class Scene(
    val ciel: EtatDuCiel = CIELS[0],
    val hauteurSoleil: Double = 52.0,
    val obstacle: Obstacle = OBSTACLES[0],
    val renvois: Set<String> = emptySet(),
    val iso: Int = 100,
    val modele: Modele = Modele.PHYSIQUE
)

/** Une ligne de la chaîne, telle qu'on l'annonce à voix haute sur le terrain. */
data class Terme(val nom: String, val detail: String, val diaphs: Double?, val ev: Double? = null)

data class Estimation(
    val hauteurSoleil: Double,
    val ev100Ciel: Double,
    val ev100Scene: Double,
    val evAppareil: Double,
    val partSoleil: Double,
    val diaphsObstacle: Double,
    val diaphsRenvoi: Double,
    val termes: List<Terme>
)

object Estimateur {

    fun calculer(scene: Scene): Estimation {
        val partSoleil: Double
        val ev100Ciel: Double

        when (scene.modele) {
            Modele.PHYSIQUE -> {
                val e = Ciel.eclairement(scene.hauteurSoleil, scene.ciel)
                ev100Ciel = e.ev100
                partSoleil = e.partSoleil
            }
            Modele.GRILLE_MENTALE -> {
                ev100Ciel = 15 + scene.ciel.plat + Ciel.perteHauteurPlate(scene.hauteurSoleil)
                partSoleil = if (scene.ciel.partDirecte > 0) 1.0 else 0.0
            }
        }

        val diaphsObstacle = when (scene.modele) {
            Modele.PHYSIQUE -> scene.obstacle.diaphs(partSoleil)
            Modele.GRILLE_MENTALE -> scene.obstacle.auSoleil
        }

        val facteur = when (scene.modele) {
            Modele.PHYSIQUE -> facteurRenvoi(partSoleil)
            Modele.GRILLE_MENTALE -> 1.0
        }

        val renvoisActifs = RENVOIS.filter { it.id in scene.renvois }
        val diaphsRenvoi = renvoisActifs.sumOf { it.diaphs * facteur }

        val ev100Scene = ev100Ciel + diaphsObstacle + diaphsRenvoi
        val evAppareil = ev100Scene + Photometrie.decalageIso(scene.iso)

        val termes = buildList {
            add(
                Terme(
                    "Lumière du ciel",
                    "${scene.ciel.nom.lowercase()} · soleil à ${arrondi(scene.hauteurSoleil)}°",
                    null,
                    ev100Ciel
                )
            )
            if (scene.obstacle.id != "ouvert") {
                val detail = if (scene.modele == Modele.PHYSIQUE && partSoleil < 0.85)
                    "${scene.obstacle.detail} · il ne reste que ${(partSoleil * 100).toInt()} % de soleil direct à couper"
                else scene.obstacle.detail
                add(Terme(scene.obstacle.nom, detail, diaphsObstacle))
            }
            renvoisActifs.forEach {
                val detail = if (scene.modele == Modele.PHYSIQUE && facteur < 0.9)
                    "atténué : peu de soleil direct" else ""
                add(Terme(it.nom, detail, it.diaphs * facteur))
            }
            add(Terme("Scène", Situations.nom(ev100Scene), null, ev100Scene))
            if (scene.iso != 100) {
                add(Terme("${scene.iso} ISO", "EV = EV₁₀₀ + log₂(ISO/100)", Photometrie.decalageIso(scene.iso)))
            }
            add(Terme("Appareil", "ce que doit lire ton couple diaph/vitesse", null, evAppareil))
        }

        return Estimation(
            hauteurSoleil = scene.hauteurSoleil,
            ev100Ciel = ev100Ciel,
            ev100Scene = ev100Scene,
            evAppareil = evAppareil,
            partSoleil = partSoleil,
            diaphsObstacle = diaphsObstacle,
            diaphsRenvoi = diaphsRenvoi,
            termes = termes
        )
    }

    private fun arrondi(x: Double) = (Math.round(x * 10) / 10.0)
}

/** Le niveau de lumière, nommé. Décrit une intensité, pas une météo. */
object Situations {
    private val ECHELLE = listOf(
        15.5 to "Neige ou sable au soleil zénithal",
        14.5 to "Plein soleil franc",
        13.5 to "Plein jour vif",
        12.5 to "Plein jour doux",
        11.5 to "Jour gris et plat",
        10.5 to "Jour sombre, ombre ouverte",
        9.5 to "Jour très sombre, sous-bois",
        8.5 to "Fin de journée",
        7.5 to "Intérieur très éclairé, vitrine",
        6.5 to "Bureau bien éclairé",
        5.5 to "Salon éclairé",
        4.5 to "Rue de nuit bien éclairée",
        2.5 to "Rue de nuit moyenne",
        0.5 to "Éclairage public faible",
        -1.5 to "Crépuscule avancé",
        -4.5 to "Pleine lune sur paysage",
        -7.5 to "Quart de lune"
    )

    fun nom(ev100: Double): String =
        ECHELLE.firstOrNull { ev100 >= it.first }?.second ?: "Nuit noire, ciel étoilé"
}
