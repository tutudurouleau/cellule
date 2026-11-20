package fr.cellule.app.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Interligne
import fr.cellule.app.RayonControle
import fr.cellule.app.TitreCarte
import fr.cellule.app.icone
import fr.cellule.core.BainCouleur
import fr.cellule.core.Dominante
import fr.cellule.core.Dominantes
import fr.cellule.core.Duree
import fr.cellule.core.FicheCouleur
import fr.cellule.core.Filtration
import fr.cellule.core.LaboCouleur
import fr.cellule.core.ProcedeCouleur
import fr.cellule.core.SequenceLabo

/*
 * Le labo couleur : une page par procédé (C-41, ECN-2, E-6, RA-4), la
 * correction d'une dominante au tirage, et la couleur expliquée. Les chiffres
 * viennent de [LaboCouleur], qui cite ses publications Kodak ; ce qui n'a
 * pas été relu y est dit, jamais comblé.
 */

/* ── Les glyphes ──────────────────────────────────────────────────────────── */

/** Une bande de film perforée : le négatif couleur. */
internal val IconePelliculeCouleur = icone("pellicule-couleur") {
    moveTo(5f, 3f); lineTo(19f, 3f); lineTo(19f, 21f); lineTo(5f, 21f); close()
    moveTo(9f, 7f); lineTo(15f, 7f); lineTo(15f, 17f); lineTo(9f, 17f); close()
    moveTo(7f, 6f); lineTo(7f, 6.5f)
    moveTo(7f, 11.75f); lineTo(7f, 12.25f)
    moveTo(7f, 17.5f); lineTo(7f, 18f)
    moveTo(17f, 6f); lineTo(17f, 6.5f)
    moveTo(17f, 11.75f); lineTo(17f, 12.25f)
    moveTo(17f, 17.5f); lineTo(17f, 18f)
}

/** Un cache de diapositive. */
internal val IconeDiapo = icone("diapo") {
    moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); lineTo(4f, 20f); close()
    moveTo(8f, 9f); lineTo(16f, 9f); lineTo(16f, 15f); lineTo(8f, 15f); close()
}

/** Trois cercles qui se croisent : la synthèse des couleurs, et les filtres. */
internal val IconeFiltreCouleur = icone("filtre-couleur") {
    moveTo(5f, 9f); arcTo(4.5f, 4.5f, 0f, true, true, 14f, 9f); arcTo(4.5f, 4.5f, 0f, true, true, 5f, 9f); close()
    moveTo(10f, 9f); arcTo(4.5f, 4.5f, 0f, true, true, 19f, 9f); arcTo(4.5f, 4.5f, 0f, true, true, 10f, 9f); close()
    moveTo(7.5f, 15f); arcTo(4.5f, 4.5f, 0f, true, true, 16.5f, 15f); arcTo(4.5f, 4.5f, 0f, true, true, 7.5f, 15f); close()
}

internal fun iconeProcede(p: ProcedeCouleur) = when (p) {
    ProcedeCouleur.C41, ProcedeCouleur.ECN2 -> IconePelliculeCouleur
    ProcedeCouleur.E6 -> IconeDiapo
    ProcedeCouleur.RA4 -> IconeTirage
}

/* ── Un procédé ───────────────────────────────────────────────────────────── */

@Composable
internal fun PageProcede(f: FicheCouleur, versChrono: (SequenceLabo) -> Unit, versDominante: () -> Unit) {
    Carte {
        Etiquette(f.procede.libelle, accent = true)
        Text(f.procede.code, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
        Text(f.procede.pour, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Paragraphe(f.resume)
    }

    val c = f.critique
    CarteResultat(titre = "Le bain qui compte") {
        GrandeValeur(c.nom, c.temperature, "${Duree.libelle(c.duree)} · ${c.role}")
    }

    Carte(
        titre = "Les bains, dans l'ordre",
        sousTitre = if (f.aVerifier > 0) "Les durées marquées « notice » n'ont pas pu être relues chez Kodak : reprends-les de la notice de ton kit."
        else null
    ) {
        f.bains.forEachIndexed { i, b -> LigneBain(i, b) }
        Separateur()
        Ligne(
            "Durée des bains",
            Duree.libelle(f.dureeConnue),
            if (f.aVerifier > 0) "sans les ${f.aVerifier} bains à reprendre de ta notice" else null
        )
        Spacer(Modifier.height(Interligne))
        BoutonPlat("Charger au chrono", accent = true, modifier = Modifier.fillMaxWidth()) { versChrono(f.sequence) }
        if (f.aVerifier > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Le chrono te demandera les durées manquantes avant de partir.",
                style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Carte(titre = "À retenir") {
        f.aRetenir.forEach { PuceCouleur(it) }
    }

    if (f.procede == ProcedeCouleur.RA4) {
        Carte(modifier = Modifier.clickable { versDominante() }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Le tirage tire vers une couleur ?", style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
                    Text("Corrige la filtration de l'agrandisseur.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("›", style = TitreCarte, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }

    Carte {
        Text(f.note, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        f.sources.forEach { LigneSource(it) }
    }
}

/** Un bain : son rang, son nom, ce qu'il fait, sa température — et sa durée à droite. */
@Composable
private fun LigneBain(i: Int, b: BainCouleur) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(
            "${i + 1}",
            style = Corps,
            color = if (b.critique) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(b.nom, style = Corps, color = if (b.critique) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if (b.temperature.isNotBlank()) {
                Text(b.temperature, style = Detail, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(b.role, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            when {
                b.aVerifier -> "notice"
                b.minimum -> "≥ " + Duree.chrono(b.duree)
                else -> Duree.chrono(b.duree)
            },
            style = Corps,
            color = if (b.aVerifier) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun PuceCouleur(texte: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("•", style = Corps, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(16.dp))
        Text(texte, style = Corps, color = MaterialTheme.colorScheme.onSurface)
    }
}

/* ── Corriger une dominante ───────────────────────────────────────────────── */

private fun teinteDominante(d: Dominante): Color = when (d) {
    Dominante.JAUNE -> Color(0xFFE8C21A)
    Dominante.MAGENTA -> Color(0xFFD0338E)
    Dominante.CYAN -> Color(0xFF19A7C9)
    Dominante.BLEU -> Color(0xFF3A5FD0)
    Dominante.VERT -> Color(0xFF3AA655)
    Dominante.ROUGE -> Color(0xFFD6453A)
}

/** Un nombre d'unités de filtre, tapé librement ; vide vaut zéro. */
private fun lireCC(t: String): Int? = if (t.isBlank()) 0 else t.trim().toIntOrNull()?.takeIf { it in 0..400 }

@Composable
internal fun OutilDominante() {
    var y by rememberSaveable { mutableStateOf("40") }
    var m by rememberSaveable { mutableStateOf("30") }
    var c by rememberSaveable { mutableStateOf("") }
    var choix by rememberSaveable { mutableStateOf<String?>(null) }
    var pas by rememberSaveable { mutableStateOf(10) }
    val dominante = choix?.let { n -> Dominante.entries.firstOrNull { it.name == n } }

    Carte(
        titre = "Ce que tu donnes",
        sousTitre = "La filtration de ta dernière bande d'essai, puis la couleur qui domine dans les gris et la peau — tirage sec, sous une lumière proche du jour."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Champ(y, "Jaune (Y)", Modifier.weight(1f), indication = "0") { y = it }
            Champ(m, "Magenta (M)", Modifier.weight(1f), indication = "0") { m = it }
            Champ(c, "Cyan (C)", Modifier.weight(1f), indication = "0") { c = it }
        }
        Spacer(Modifier.height(Interligne + 4.dp))
        Etiquette("Le tirage est trop…")
        Spacer(Modifier.height(6.dp))
        GrilleDominantes(dominante) { choix = it.name }
        Spacer(Modifier.height(Interligne + 4.dp))
        Etiquette("De combien (unités CC)")
        Spacer(Modifier.height(6.dp))
        ChoixSegmente(Dominantes.PAS, pas, { "$it" }) { pas = it }
        Spacer(Modifier.height(6.dp))
        Text(
            "Juge à l'œil : une dominante à peine visible demande peu. Mieux vaut deux bandes qu'une correction qui bascule de l'autre côté.",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val fy = lireCC(y)
    val fm = lireCC(m)
    val fc = lireCC(c)
    when {
        fy == null || fm == null || fc == null -> Carte {
            BandeauEtat("Une filtration se donne en nombres entiers, de 0 à 400.", alerte = true)
        }
        dominante == null -> Carte {
            BandeauEtat("Choisis la couleur qui domine sur ton tirage.", alerte = false)
        }
        else -> {
            val r = Dominantes.corriger(Filtration(fy, fm, fc), dominante, pas)
            CarteResultat(titre = "Nouvelle filtration") {
                GrandeValeur(r.geste, r.apres.libelle, "au lieu de ${r.avant.libelle}")
                Spacer(Modifier.height(4.dp))
                Text(dominante.pourquoi, style = Detail)
                if (r.neutreAjoute > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Un filtre ne descend pas sous zéro : on ajoute ${r.neutreAjoute} aux trois. Cette densité neutre ne change pas la couleur — d'où le cyan.",
                        style = Detail
                    )
                }
                if (r.neutreAjoute < 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tes trois filtres avaient ${-r.neutreAjoute} en commun : une densité neutre qui n'allonge que la pose. Elle est retirée ici.",
                        style = Detail
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("La filtration change la pose : refais une bande d'essai avant de tirer.", style = Detail)
            }
        }
    }

    Carte {
        Depliable("La règle des dominantes") {
            Paragraphe(
                "En tirant un négatif, on ajoute un filtre de la couleur de la dominante. Un filtre absorbe sa complémentaire : " +
                    "la couche du papier qui forme ce colorant reçoit moins de lumière, et en forme moins."
            )
            Dominante.entries.forEach { d -> Ligne("Trop ${d.libelle}", d.geste) }
            Spacer(Modifier.height(6.dp))
            Paragraphe(
                "On travaille avec deux filtres sur trois, le cyan à zéro : les trois ensemble ne font qu'ajouter du gris, et allonger la pose."
            )
            Paragraphe(
                "Une diapositive tirée sur papier inversible suit la règle contraire : on retire la couleur de la dominante."
            )
        }
    }
}

/** Les six dominantes, en deux rangées : chaque colonne réunit deux complémentaires. */
@Composable
private fun GrilleDominantes(selection: Dominante?, surChoix: (Dominante) -> Unit) {
    val rangees = listOf(
        listOf(Dominante.JAUNE, Dominante.MAGENTA, Dominante.CYAN),
        listOf(Dominante.BLEU, Dominante.VERT, Dominante.ROUGE)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rangees.forEach { rangee ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rangee.forEach { d ->
                    val actif = d == selection
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(RayonControle))
                            .background(
                                if (actif) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { surChoix(d) }
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(teinteDominante(d)))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            d.libelle,
                            style = Corps,
                            maxLines = 1,
                            color = if (actif) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/* ── La couleur expliquée ─────────────────────────────────────────────────── */

@Composable
internal fun ComprendreCouleur(ouvrirDominante: () -> Unit) {
    Carte(titre = "1. Trois couches, trois colorants") {
        Paragraphe(
            "Un film couleur superpose trois émulsions noir et blanc, chacune sensible à un tiers du spectre. " +
                "Au développement, chacune forme le colorant complémentaire de la lumière qu'elle a reçue."
        )
        Ligne("Couche sensible au bleu", "jaune")
        Ligne("Couche sensible au vert", "magenta")
        Ligne("Couche sensible au rouge", "cyan")
        Paragraphe("Voilà pourquoi un négatif montre les couleurs inversées : un ciel bleu y devient jaune.")
    }
    Carte(titre = "2. Le révélateur fabrique la couleur") {
        Paragraphe(
            "Le révélateur chromogène développe l'argent comme en noir et blanc. Mais en s'oxydant, il se lie aux coupleurs " +
                "logés dans chaque couche : c'est cette union qui forme le colorant. Plus d'argent développé, plus de colorant."
        )
    }
    Carte(titre = "3. Le blanchiment, puis le fixage") {
        Paragraphe(
            "À la sortie du révélateur, l'image contient à la fois de l'argent et des colorants. Le blanchiment reconvertit l'argent " +
                "en un sel que le fixateur dissout ensuite. Il ne reste que les colorants."
        )
        Paragraphe("Sans blanchiment, l'argent reste dans l'image : elle sort grise et dense. Le RA-4 réunit les deux bains en un seul, le blanchiment-fixage.")
    }
    Carte(titre = "4. La température, au dixième") {
        Paragraphe(
            "Les trois couches doivent garder le même contraste. Une température fausse ne change pas seulement la densité : " +
                "les couleurs dérivent des ombres aux hautes lumières, et aucun filtre ne rattrape une dominante qui change avec la densité."
        )
        LaboCouleur.FICHES.forEach { f ->
            Ligne(f.procede.code, f.critique.temperature, "${f.critique.nom.lowercase()} · ${Duree.libelle(f.critique.duree)}")
        }
        Paragraphe("Bain-marie, thermomètre précis, bains préchauffés avant de verser : c'est tout le secret.")
    }
    Carte(titre = "5. Le masque orange") {
        Paragraphe(
            "Les colorants réels ne sont pas parfaits : le magenta et le cyan absorbent un peu de la lumière qu'ils devraient laisser passer. " +
                "Des coupleurs colorés compensent ce défaut dans le négatif — c'est le masque orange. Le papier couleur et le scan sont faits pour l'annuler."
        )
    }
    Carte(titre = "6. Négatif ou diapositive") {
        Paragraphe(
            "Le C-41 et l'ECN-2 donnent un négatif, qu'on tire ensuite. Le E-6 donne directement une image positive : un premier révélateur " +
                "noir et blanc fait le négatif, le bain d'inversion voile les grains restés intacts, et le révélateur chromogène les transforme en positif coloré."
        )
        Paragraphe("Conséquence : en diapo, la densité se décide au premier révélateur, et la pose doit être juste à la prise de vue.")
    }
    Carte(titre = "7. Les dominantes au tirage", modifier = Modifier.clickable { ouvrirDominante() }) {
        Paragraphe(
            "À l'agrandisseur couleur, trois filtres soustractifs — jaune, magenta, cyan — dosent la lumière que reçoit chaque couche du papier. " +
                "Règle du négatif : on ajoute la couleur de la dominante."
        )
        Text("→ Corriger une dominante", style = Corps, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 6.dp))
    }
    Carte(titre = "Avant de t'y mettre") {
        PuceCouleur("Gants et aération : les bains couleur irritent la peau. Lis la fiche de sécurité de ton kit.")
        PuceCouleur("Le film couleur se charge dans le noir complet, comme le noir et blanc.")
        PuceCouleur("Les durées d'un kit d'une autre marque que Kodak peuvent différer : sa notice fait foi.")
    }
}

/** Les quatre révélateurs, d'un coup d'œil : en tête de l'accueil couleur. */
@Composable
internal fun CarteRevelateursCouleur(ouvrir: (ProcedeCouleur) -> Unit) {
    Carte(titre = "Les révélateurs, d'un coup d'œil", sousTitre = "Le bain qui décide de l'image, pour chaque procédé — touche pour tout voir.") {
        LaboCouleur.FICHES.forEach { f ->
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(RayonControle)).clickable { ouvrir(f.procede) }) {
                Ligne(
                    "${f.procede.code} · ${f.procede.libelle.lowercase()}",
                    f.critique.temperature,
                    "${f.critique.nom} : ${Duree.libelle(f.critique.duree)}"
                )
            }
        }
    }
}
