package fr.cellule.app.ecrans

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cellule.app.Interligne
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.core.CIELS
import fr.cellule.core.FILTRES_ND
import fr.cellule.core.MATIERES
import fr.cellule.core.OBSTACLES
import fr.cellule.core.Photometrie
import fr.cellule.core.RENVOIS
import fr.cellule.core.SENSIBILITES
import fr.cellule.core.Situations
import fr.cellule.core.ZONES
import fr.cellule.core.coupleDeTable
import fr.cellule.core.libelleDiaphs
import fr.cellule.core.libelleOuverture

@Composable
fun EcranTables() {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)
    ) {

        Carte(
            titre = "La table maîtresse — 100 ISO",
            sousTitre = "Ce qui arrive sur la scène, ce que tu affiches sur l'appareil, et à quoi ça ressemble. Sunny 16 est la ligne EV 15."
        ) {
            Tableau(
                listOf(Colonne("EV", 0.55f), Colonne("Réglage", 1.3f), Colonne("Éclairement", 1.1f, aDroite = true)),
                listOf(16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 3, 0, -3, -6).map { ev ->
                    val c = coupleDeTable(ev.toDouble())
                    RangTableau(
                        listOf(
                            "$ev",
                            c?.let { "${libelleOuverture(it.ouverture)} · ${it.vitesse.libelle}" } ?: "—",
                            fmtLux(Photometrie.lux(ev.toDouble()))
                        ),
                        detail = Situations.nom(ev.toDouble()),
                        accent = ev == 15
                    )
                }
            )
        }

        Carte(
            titre = "Les facteurs cumulables",
            sousTitre = "Départ : plein soleil, soleil haut, terrain ouvert, sujet à 18 % → EV 15, soit f/16 au 1/ISO. Tout le reste s'ajoute en diaphs."
        ) {
            val colonnes = { titre: String -> listOf(Colonne(titre, 2f), Colonne("Diaphs", 0.8f, aDroite = true)) }
            Tableau(
                colonnes("Ciel"),
                CIELS.filter { it.plat != 0.0 }.map { RangTableau(listOf(it.nom, signe(it.plat)), detail = it.detail) }
            )
            Spacer(Modifier.height(Interligne))
            Tableau(
                colonnes("Obstacle"),
                OBSTACLES.filter { it.auSoleil != 0.0 }.map { RangTableau(listOf(it.nom, signe(it.auSoleil)), detail = it.detail) }
            )
            Spacer(Modifier.height(Interligne))
            Tableau(colonnes("Renvoi"), RENVOIS.map { RangTableau(listOf(it.nom, signe(it.diaphs))) })
        }

        Carte(
            titre = "Réflectances",
            sousTitre = "L'écart au gris 18 % vaut log₂(ρ / 0,18). C'est la correction à faire sur toute lecture réfléchie."
        ) {
            Tableau(
                listOf(Colonne("Matière", 1.6f), Colonne("ρ", 0.7f, aDroite = true), Colonne("Écart", 0.8f, aDroite = true)),
                MATIERES.map {
                    RangTableau(
                        listOf(it.nom, fmt(it.reflectance, 3), libelleDiaphs(it.ecartAuGris)),
                        accent = it.reflectance == 0.18
                    )
                }
            )
            Text(
                "Attention à l'eau : sa réflectance diffuse est très basse, mais sa composante spéculaire approche 1,0. Une rivière au soleil rasant renvoie à la fois presque rien et presque tout — d'où sa dynamique énorme, et le piège qu'elle tend à toute cellule moyenne.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Carte(
            titre = "Le zone system",
            sousTitre = "Une zone = un diaph. La zone V est ce que donne toute cellule, quelle que soit la matière visée."
        ) {
            Tableau(
                listOf(Colonne("Zone", 0.6f), Colonne("Réflectance", 1f, aDroite = true)),
                ZONES.map {
                    RangTableau(
                        listOf(it.chiffre, fmt(it.reflectance * 100, 1) + " %"),
                        detail = it.rendu,
                        accent = it.ecart == 0
                    )
                }
            )
        }

        Carte(titre = "Sensibilité", sousTitre = "EV = EV₁₀₀ + log₂(ISO/100).") {
            Tableau(
                listOf(Colonne("ISO", 0.7f), Colonne("Écart", 0.6f, aDroite = true), Colonne("Sunny 16", 1.4f, aDroite = true)),
                SENSIBILITES.map { iso ->
                    val c = coupleDeTable(15.0 + Photometrie.decalageIso(iso))
                    RangTableau(
                        listOf(
                            "$iso",
                            signe(Photometrie.decalageIso(iso), 0),
                            c?.let { "${libelleOuverture(it.ouverture)} · ${it.vitesse.libelle}" } ?: "—"
                        ),
                        accent = iso == 100
                    )
                }
            )
        }

        Carte(
            titre = "Filtres neutres",
            sousTitre = "Règle des 180° : t = 1 / (2 × fps). La vitesse étant fixée, il ne reste que l'ouverture, l'EI et les ND."
        ) {
            Tableau(
                listOf(Colonne("Filtre", 1.4f), Colonne("Retire", 1f, aDroite = true)),
                FILTRES_ND.filter { it.diaphs > 0 }.map {
                    RangTableau(listOf(it.libelle, "${it.diaphs} diaph" + if (it.diaphs > 1) "s" else ""))
                }
            )
            Text(
                "25 fps, 800 EI, plein soleil : EV 18 à 1/50 demande f/64. Un ND 1,2 te ramène à f/18, un ND 1,8 à f/9.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Carte(titre = "Les six relations") {
            listOf(
                "L = E × ρ / π" to "luminance depuis éclairement et matière",
                "EV₁₀₀ = log₂(E / 2,5)" to "éclairement incident → EV",
                "EV₁₀₀ = log₂(L / 0,125)" to "luminance réfléchie → EV",
                "EV = log₂(N² / t)" to "réglages → EV",
                "ΔEV = log₂(ρ / 0,18)" to "correction de réflectance",
                "L* = 116 × Y^⅓ − 16" to "luminance physique → clarté perçue"
            ).forEach { (formule, sens) -> Ligne(formule, "", sens) }
            Text(
                "Les cinq premières font le lien entre le monde et l'appareil. La sixième fait le lien entre le monde et ton œil — c'est celle qui explique pourquoi ton intuition et ton posemètre ne sont jamais tout à fait d'accord.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Carte(titre = "Ce que ces chiffres ne disent pas") {
            Text(
                "C = 250 et K = 12,5 ne sont pas cohérentes entre elles : la cohérence exigerait C ≈ 218. Il reste 0,2 diaph d'écart structurel entre une cellule incidente et un spotmètre sur charte grise.\n\n" +
                    "Le modèle atmosphérique suppose un ciel clair. Brume de chaleur, poussière, fumée : chacune retire de ½ à 2 diaphs sans prévenir, et l'œil s'y adapte trop bien pour les voir.\n\n" +
                    "Les réflectances sont des médianes. De l'asphalte mouillé chute vers 0,05 ; de l'herbe sèche perd 0,10 sur de l'herbe irriguée.\n\n" +
                    "Et en cas de doute avec du négatif : surexpose. L'asymétrie est réelle et large.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
