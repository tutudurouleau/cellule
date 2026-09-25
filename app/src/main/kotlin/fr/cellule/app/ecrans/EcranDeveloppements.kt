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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.DepotLabo
import fr.cellule.app.Detail
import fr.cellule.app.EtatApplication
import fr.cellule.app.Interligne
import fr.cellule.core.CarnetLabo
import fr.cellule.core.DeveloppementNote
import fr.cellule.core.Duree
import fr.cellule.core.Resultat
import java.time.LocalDate
import kotlin.math.roundToInt

private fun tempsCarnet(secondes: Double?): String = secondes?.let { Duree.libelle(it) } ?: ""

private fun ecart(x: Double): String {
    val p = (x * 100).roundToInt()
    return if (p == 0) "juste" else (if (p > 0) "+" else "−") + kotlin.math.abs(p) + " %"
}

/**
 * Le carnet de développement : pour chaque film, ton estimation avant le
 * calcul, le calcul, le temps réellement donné, puis ce que le négatif a
 * montré. Relu au fil des films, c'est la preuve que l'intuition se forme.
 */
@Composable
fun CarnetDeveloppements(labo: DepotLabo, etat: EtatApplication) {
    var film by remember { mutableStateOf("") }
    var ei by remember { mutableStateOf("") }
    var revelateur by remember { mutableStateOf("") }
    var dilution by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("20") }
    var estimation by remember { mutableStateOf("") }
    var calcule by remember { mutableStateOf("") }
    var donne by remember { mutableStateOf("") }
    var cuve by remember { mutableStateOf("") }
    var agitation by remember { mutableStateOf("") }
    var resultat by remember { mutableStateOf<Resultat?>(null) }
    var notes by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var toutVoir by remember { mutableStateOf(false) }

    /* Ce que le Labo a préparé : calculateur ou chrono. */
    LaunchedEffect(etat.developpementPropose) {
        etat.developpementPropose?.let { p ->
            if (p.film.isNotBlank()) film = p.film
            p.ei?.let { ei = "$it" }
            if (p.revelateur.isNotBlank()) revelateur = p.revelateur
            if (p.dilution.isNotBlank()) dilution = p.dilution
            p.temperature?.let { temperature = fmt(it, 1).removeSuffix(",0") }
            p.tempsCalcule?.let { calcule = Duree.chrono(it) }
            p.estimation?.let { estimation = Duree.chrono(it) }
            p.tempsDonne?.let { donne = Duree.chrono(it) }
            if (p.agitation.isNotBlank()) agitation = p.agitation
            etat.developpementPropose = null
            message = "Repris du Labo. Complète, puis note le résultat une fois le film sec."
        }
    }

    Carte(
        titre = "Noter un développement",
        sousTitre = "Trois temps côte à côte — ton estimation avant de calculer, le calcul, ce que tu as donné — puis ce que le négatif a montré."
    ) {
        LigneBoutons {
            Champ(film, "Film", Modifier.weight(2f), indication = "Ilford HP5 Plus") { film = it }
            Champ(ei, "EI", Modifier.weight(1f), indication = "400") { ei = it }
        }
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Champ(revelateur, "Révélateur", Modifier.weight(1.4f), indication = "ID-11") { revelateur = it }
            Champ(dilution, "Dilution", Modifier.weight(1f), indication = "1+1") { dilution = it }
        }
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Champ(temperature, "°C", Modifier.weight(1f)) { temperature = it }
            Champ(cuve, "Cuve", Modifier.weight(2f), indication = "2 spires, 500 mL") { cuve = it }
        }
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Champ(estimation, "Estimé", Modifier.weight(1f), indication = "m:ss") { estimation = it }
            Champ(calcule, "Calculé", Modifier.weight(1f), indication = "m:ss") { calcule = it }
            Champ(donne, "Donné", Modifier.weight(1f), indication = "m:ss") { donne = it }
        }
        Spacer(Modifier.height(Interligne))
        Champ(agitation, "Agitation", Modifier.fillMaxWidth(), indication = "Ilford, 10 s par minute") { agitation = it }
        Spacer(Modifier.height(Interligne))
        Deroulant(
            intitule = "Résultat",
            options = listOf<Resultat?>(null) + Resultat.entries,
            selection = resultat,
            libelle = { it?.libelle ?: "Pas encore vu" },
            modifier = Modifier.fillMaxWidth()
        ) { resultat = it }
        Spacer(Modifier.height(Interligne))
        Champ(notes, "Notes", Modifier.fillMaxWidth(), surUneLigne = false, indication = "densité des ombres, grain, taches…") { notes = it }
        Spacer(Modifier.height(Interligne + 2.dp))
        BoutonPlat("Enregistrer le développement", accent = true, modifier = Modifier.fillMaxWidth()) {
            if (film.isBlank() && revelateur.isBlank() && donne.isBlank()) {
                message = "Rien à enregistrer : donne au moins le film, le révélateur ou le temps."
            } else {
                val n = DeveloppementNote(
                    identifiant = System.currentTimeMillis(),
                    date = LocalDate.now().toString(),
                    film = film.trim(),
                    ei = ei.trim().toIntOrNull(),
                    revelateur = revelateur.trim(),
                    dilution = dilution.trim(),
                    temperature = temperature.replace(',', '.').trim().toDoubleOrNull(),
                    tempsCalcule = Duree.lire(calcule),
                    estimation = Duree.lire(estimation),
                    tempsDonne = Duree.lire(donne),
                    cuve = cuve.trim(),
                    agitation = agitation.trim(),
                    resultat = resultat,
                    notes = notes.trim()
                )
                labo.ajouter(n)
                message = n.ecartEstimation?.let { "Enregistré. Ton estimation : ${ecart(it)} par rapport au calcul." }
                    ?: "Enregistré."
                estimation = ""; calcule = ""; donne = ""; resultat = null; notes = ""
            }
        }
        Spacer(Modifier.height(Interligne))
        BandeauEtat(message, alerte = message.startsWith("Rien"))
    }

    val bilan = CarnetLabo.bilan(labo.notes)
    if (bilan != null) {
        Carte(titre = "Ce que disent tes négatifs", sousTitre = "${bilan.nombre} film(s) développé(s)") {
            if (bilan.nombreEstimes > 0) {
                Ligne(
                    "Ton intuition",
                    if (bilan.nombreEstimes > 5) "${ecart(bilan.erreurDebut ?: 0.0).removePrefix("+")} → ${ecart(bilan.erreurRecente ?: 0.0).removePrefix("+")}"
                    else ecart(bilan.erreurRecente ?: 0.0).removePrefix("+"),
                    if (bilan.nombreEstimes > 5) "erreur moyenne des 5 premières estimations aux 5 dernières"
                    else "erreur moyenne sur ${bilan.nombreEstimes} estimation(s) ; la tendance viendra"
                )
            }
            Resultat.entries.forEach { r ->
                bilan.resultats[r]?.let { Ligne(r.libelle, "$it") }
            }
            /* Le conseil suit le défaut le plus fréquent : c'est lui qui parle de ta chaîne. */
            bilan.resultats.filterKeys { it != Resultat.CORRECT }.maxByOrNull { it.value }?.let { (r, _) ->
                CarnetLabo.conseil(r)?.let { (texte, source) ->
                    Separateur()
                    Etiquette("Le plus fréquent : ${r.libelle.lowercase()}")
                    Paragraphe(texte)
                    source?.let { LigneSource(it) }
                }
            }
        }
    }

    Carte(titre = "Les films développés") {
        val notesListe = labo.notes
        if (notesListe.isEmpty()) {
            Text(
                "Rien encore. Après un calcul ou un chrono, « Verser au carnet » prépare la fiche.",
                style = Corps,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val visibles = if (toutVoir) notesListe.reversed() else notesListe.reversed().take(8)
            visibles.forEach { n -> LigneDeveloppement(n) { labo.supprimer(n.identifiant) } }
            if (notesListe.size > 8) {
                Spacer(Modifier.height(Interligne))
                BoutonPlat(if (toutVoir) "Réduire" else "Tout voir (${notesListe.size})") { toutVoir = !toutVoir }
            }
        }
    }
}

@Composable
private fun LigneDeveloppement(n: DeveloppementNote, surSuppression: () -> Unit) {
    var confirme by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(n.titre, style = Corps, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    listOfNotNull(
                        n.date,
                        n.temperature?.let { fmt(it, 1).removeSuffix(",0") + " °C" },
                        n.cuve.ifBlank { null }
                    ).joinToString(" · "),
                    style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(Modifier.clickable { if (confirme) surSuppression() else confirme = true }.padding(start = 10.dp, top = 2.dp)) {
                Text(
                    if (confirme) "confirmer" else "✕",
                    style = Detail,
                    color = if (confirme) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val tempsLigne = listOfNotNull(
            n.estimation?.let { "estimé ${tempsCarnet(it)}" },
            n.tempsCalcule?.let { "calculé ${tempsCarnet(it)}" },
            n.tempsDonne?.let { "donné ${tempsCarnet(it)}" }
        )
        if (tempsLigne.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(
                tempsLigne.joinToString("  ·  ") + (n.ecartEstimation?.let { "  ·  intuition ${ecart(it)}" } ?: ""),
                style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val fin = listOfNotNull(n.resultat?.libelle, n.agitation.ifBlank { null }, n.notes.ifBlank { null })
        if (fin.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(
                fin.joinToString("  ·  "),
                style = Detail,
                color = if (n.resultat == Resultat.CORRECT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
