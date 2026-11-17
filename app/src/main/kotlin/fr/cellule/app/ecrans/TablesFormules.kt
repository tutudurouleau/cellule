package fr.cellule.app.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.TitreCarte
import fr.cellule.core.AMBIANCES
import fr.cellule.core.CIRCUITS
import fr.cellule.core.FORMATS_PELLICULE
import fr.cellule.core.FamilleGelatine
import fr.cellule.core.GELATINES
import fr.cellule.core.MODES_CAPTEUR
import fr.cellule.core.PELLICULES_CINE
import fr.cellule.core.PROJECTEURS_CATALOGUE
import fr.cellule.core.Region
import fr.cellule.core.nombreCourt

/*
 * Toutes les formules et toutes les tables dont l'appli se sert, au même
 * endroit, avec leurs sources : ce qu'elle calcule ne doit jamais être une
 * boîte noire.
 */

@Composable
private fun ColumnScope.Formule(nom: String, ecriture: String, explication: String? = null) {
    Spacer(Modifier.height(8.dp))
    Text(nom, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
    Text(
        ecriture,
        style = Corps.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
    if (explication != null) {
        Text(explication, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun n(x: Double, d: Int = 2) = nombreCourt(x, d)

@Composable
fun TablesFormules() {
    Carte(
        titre = "Ce que l'appli calcule",
        sousTitre = "Chaque formule utilisée par Cellule, et chaque table de données avec sa source. Les valeurs « à relire » n'ont pas encore été retrouvées sur une fiche publiée."
    ) {}

    Carte(titre = "Exposition") {
        Formule("Indice de lumination", "EV = log₂(N² / t)", "N : ouverture, t : temps de pose en secondes. À 100 ISO.")
        Formule("Sensibilité", "EV(ISO) = EV₁₀₀ + log₂(ISO / 100)", "Doubler la sensibilité gagne un diaph.")
        Formule("Cellule incidente", "E (lux) = 2,5 × 2^EV₁₀₀", "Constante d'étalonnage C = 250 (norme ISO 2720, plage 240 à 400).")
        Formule("Cellule réfléchie", "L (cd/m²) = 0,125 × 2^EV₁₀₀", "Constante K = 12,5. Les deux voies diffèrent d'environ 0,2 diaph sur une charte grise (C = K·π/ρ ≈ 218 serait cohérent).")
        Formule("Ouverture pour une vitesse", "N = √(2^EV × t)")
        Formule("Diaphs", "écart (diaphs) = log₂(rapport) ; rapport = 2^diaphs", "Un diaph double ou divise la lumière par deux ; un tiers de diaph vaut ×1,26.")
    }

    Carte(titre = "Tournage") {
        Formule("Obturateur", "t = angle / (360 × cadence)", "180° à 25 im/s : 1/50 s ; à 24 im/s : 1/48 s.")
        Formule("T-stop qui expose juste", "T = √(2^(EV₁₀₀ + log₂(EI/100) − pertes) × t)", "Pertes : neutres et filtres posés devant l'objectif, en diaphs.")
        Formule("Densité neutre", "densité = diaphs × log₁₀2 ≈ 0,3 × diaphs ; transmission = 2^−diaphs", "ND 0,3 = 1 diaph, ND 0,6 = 2, ND 0,9 = 3.")
        Formule("T et f", "T = f / √(transmission de l'objectif)")
    }

    Carte(titre = "Lumière") {
        Formule("Intensité d'une source", "I (cd) = E₀ × d₀²", "Depuis un éclairement E₀ mesuré dans l'axe à la distance d₀.")
        Formule("Loi du carré de la distance", "E = I × cos θ / d² ÷ 2^pertes", "θ : angle d'arrivée sur le sujet. Valable au-delà d'environ 5 fois la taille de la source.")
        Formule("Distance pour un éclairement", "d = √(I × cos θ / (E × 2^pertes))")
        Formule("Éclairement pour un T voulu", "E = 2,5 × 2^(log₂(T²/t) − log₂(EI/100) + pertes)")
        Formule("Tache de lumière", "D = 2 × d × tan(faisceau / 2) ; d = largeur / (2 × tan(faisceau / 2))")
        Formule("Géométrie", "d = √(recul² + (h_projecteur − h_sujet)²) ; inclinaison = atan(Δh / recul)")
        Formule("Plusieurs sources", "E_total = E₁ + E₂ + …", "Les éclairements s'additionnent ; les diaphs, eux, ne s'additionnent pas.")
        Formule("Rapports", "face / débouchage = E_face / E_déb ; côté éclairé / côté ombre = (E_face + E_déb) / E_déb")
        Formule("Unités", "1 foot-candle = 10,764 lux ; 1 pied = 0,3048 m")
    }

    Carte(titre = "Couleur") {
        Formule("Mired", "mired = 1 000 000 / K")
        Formule("Décalage à corriger", "Δ = 10⁶/K_cible − 10⁶/K_source", "Positif : vers le chaud (orange). Négatif : vers le froid (bleu). Les décalages des gélatines s'additionnent en mireds.")
        Formule("Perte d'une gélatine", "diaphs = −log₂(transmission)")
    }

    Carte(titre = "Optique et cadre") {
        Formule("Angle de champ", "α = 2 × atan(dimension / (2 × f))", "Dimension du capteur dans le sens voulu ; multipliée par le facteur d'anamorphose en largeur.")
        Formule("Focale pour un angle", "f = dimension / (2 × tan(α / 2))")
        Formule("Ce que couvre le cadre", "largeur = distance × capteur / f ; f = capteur × distance / largeur")
        Formule("Cercle de confusion (convention)", "c = diagonale du capteur / 1500")
        Formule("Hyperfocale", "H = f² / (N × c) + f")
        Formule("Profondeur de champ", "proche = s(H − f) / (H + s − 2f) ; loin = s(H − f) / (H − s), l'infini si s ≥ H")
        Formule("Cadre sur la photo du téléphone", "fraction = (dimension / 2f) / tan(champ du téléphone / 2)", "Les tangentes des demi-angles se comparent directement : c'est ce qui se projette à plat.")
    }

    Carte(titre = "Électricité") {
        Formule("Monophasé", "I = P / (U × cos φ)")
        Formule("Triphasé", "I = P / (√3 × U × cos φ)", "U : tension entre phases (400 V en Europe, 208 V aux États-Unis).")
        Formule("Groupe électrogène", "kW = 0,8 × kVA", "Facteur de puissance nominal des groupes.")
        Formule("Marge", "charge ≤ 80 % du calibre", "Règle NEC aux États-Unis pour les charges continues ; prudence ailleurs.")
    }

    Carte(titre = "Scintillement") {
        Formule("Battement des sources", "f_battement = 2 × f_secteur", "100 Hz en Europe (50 Hz), 120 Hz aux États-Unis (60 Hz).")
        Formule("Sûr", "t × f_battement = nombre entier")
        Formule("Sûr si la cadence est stable", "f_battement / cadence = nombre entier")
        Formule("Angles sûrs", "angle = k × 360 × cadence / f_battement", "24 im/s sur 50 Hz : 86,4°, 172,8°, 259,2°, 345,6°.")
    }

    Carte(titre = "Soleil") {
        Formule("Déclinaison (série de Spencer)", "δ = 0,006918 − 0,399912 cos g + 0,070257 sin g − 0,006758 cos 2g + 0,000907 sin 2g − 0,002697 cos 3g + 0,00148 sin 3g", "g = 2π (jour − 1) / 365.")
        Formule("Équation du temps (minutes)", "E = 229,18 × (0,000075 + 0,001868 cos g − 0,032077 sin g − 0,014615 cos 2g − 0,040849 sin 2g)")
        Formule("Temps solaire", "t_sol = heure légale − fuseau + longitude / 15 + E / 60")
        Formule("Hauteur", "sin h = sin φ sin δ + cos φ cos δ cos H ; H = 15° × (t_sol − 12)")
        Formule("Azimut (depuis le nord)", "A = atan2(sin H, cos H sin φ − tan δ cos φ) + 180°")
        Formule("Heures dorée et bleue (convention)", "dorée : h entre −4° et +6° ; bleue : h entre −6° et −4°")
    }

    Carte(titre = "Pellicules de cinéma", sousTitre = "Indices publiés par Kodak, filtre de conversion compris.") {
        Tableau(
            listOf(Colonne("Pellicule", 1.6f), Colonne("Jour", 0.6f, aDroite = true), Colonne("Tungst.", 0.7f, aDroite = true)),
            PELLICULES_CINE.map { p ->
                RangTableau(
                    listOf(p.nom.removePrefix("Kodak ").removePrefix("Eastman "), "EI ${p.eiJour}" + p.filtreJour.let { if (it.isBlank()) "" else " ($it)" }, "EI ${p.eiTungstene}" + p.filtreTungstene.let { if (it.isBlank()) "" else " ($it)" }),
                    detail = p.codes + " · " + p.source
                )
            },
            petit = true
        )
    }

    Carte(titre = "Fenêtres de prise de vues") {
        Tableau(
            listOf(Colonne("Format", 1.7f), Colonne("mm", 1f, aDroite = true)),
            FORMATS_PELLICULE.map { RangTableau(listOf(it.nom, n(it.largeurMm) + " × " + n(it.hauteurMm)), detail = it.source) },
            petit = true
        )
    }

    Carte(titre = "Modes de capteur") {
        Tableau(
            listOf(Colonne("Caméra, mode", 1.7f), Colonne("mm", 1f, aDroite = true)),
            MODES_CAPTEUR.flatMap { c ->
                c.modes.map { z ->
                    RangTableau(listOf(c.camera + " — " + z.nom, n(z.largeurMm) + " × " + n(z.hauteurMm)), detail = (if (z.relue) "" else "à relire · ") + z.source)
                }
            },
            petit = true
        )
    }

    Carte(titre = "Gélatines de correction", sousTitre = "Décalage en mireds et transmission, quand ils sont publiés.") {
        listOf("Lee", "Rosco").forEach { marque ->
            Etiquette(marque)
            Tableau(
                listOf(Colonne("Gélatine", 1.6f), Colonne("Mireds", 0.6f, aDroite = true), Colonne("Transm.", 0.6f, aDroite = true)),
                GELATINES.filter { it.marque == marque && (it.famille == FamilleGelatine.VERS_CHAUD || it.famille == FamilleGelatine.VERS_FROID) }.map { g ->
                    RangTableau(
                        listOf(g.reference + " " + g.nom, g.mireds?.let { (if (it > 0) "+" else "") + n(it, 0) } ?: "—", g.transmission?.let { n(it * 100, 0) + " %" } ?: "—"),
                        detail = if (g.relue) null else "à relire"
                    )
                },
                petit = true
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    Carte(titre = "Projecteurs", sousTitre = "Éclairement publié dans l'axe, à la distance indiquée.") {
        Tableau(
            listOf(Colonne("Projecteur", 1.6f), Colonne("Lux", 0.8f, aDroite = true), Colonne("à", 0.5f, aDroite = true)),
            PROJECTEURS_CATALOGUE.flatMap { p ->
                p.mesures.map { m ->
                    RangTableau(
                        listOf(p.nomComplet + if (p.mesures.size > 1) (if (m.spot) " (serré)" else " (ouvert)") else "", n(m.lux, 0), n(m.distanceM, 0) + " m"),
                        detail = listOfNotNull(p.angle(m.spot)?.let { n(it, 0) + "°" }, if (p.relu) null else "à relire", p.source).joinToString(" · ")
                    )
                }
            },
            petit = true
        )
    }

    Carte(titre = "Circuits électriques") {
        Tableau(
            listOf(Colonne("Circuit", 1.5f), Colonne("Tension", 0.7f, aDroite = true), Colonne("Puissance", 0.8f, aDroite = true)),
            CIRCUITS.map { c ->
                RangTableau(
                    listOf(c.nom, n(c.tension, 0) + " V" + if (c.triphase) " tri" else "", n(c.puissance / 1000, 1) + " kVA"),
                    detail = if (c.region == Region.US) "États-Unis" else "Europe"
                )
            },
            petit = true
        )
    }

    Carte(titre = "Ambiances (repères)") {
        Tableau(
            listOf(Colonne("Ambiance", 1f), Colonne("Face : déb.", 1f, aDroite = true)),
            AMBIANCES.map { a -> RangTableau(listOf(a.nom, n(a.rapportMin, 0) + ":1 à " + n(a.rapportMax, 0) + ":1"), detail = a.idee) },
            petit = true
        )
    }
    Column(Modifier.padding(bottom = 8.dp)) {}
}
