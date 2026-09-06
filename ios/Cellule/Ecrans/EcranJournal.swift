import SwiftUI

struct EcranJournal: View {

    @EnvironmentObject private var depot: DepotJournal
    @EnvironmentObject private var reglages: Reglages
    @EnvironmentObject private var etat: EtatApplication

    @State private var pellicule: String = ""
    @State private var vue: String = ""
    @State private var sujet: String = ""
    @State private var type: String = TYPES_DE_SCENE[0]
    @State private var annonce: String = ""
    @State private var mesure: String = ""
    @State private var reglage: String = ""
    @State private var note: String = ""
    @State private var message: String = ""
    @State private var toutVoir = false

    private var bilan: Bilan? { Statistiques.bilan(depot.entrees) }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: Espace.interligne)
                carteSaisie
                carteBiais
                carteCarnet
            }
            .padding(.bottom, 20)
        }
        .background(Teinte.fond)
        .onAppear {
            if pellicule.isEmpty { pellicule = reglages.pellicule }
            if vue.isEmpty { vue = Statistiques.vueSuivante(depot.entrees, pellicule: pellicule) }
        }
        /* Ce que les autres écrans ont préparé. */
        .onChange(of: etat.annonceProposee) { proposee in
            guard let proposee = proposee else { return }
            annonce = fmt(proposee).replacingOccurrences(of: ",", with: ".")
            type = etat.dernierTypeScene
            if note.isEmpty { note = etat.derniereChaine }
            etat.annonceProposee = nil
            message = "Estimation reprise. Mesure maintenant, puis enregistre."
        }
        .onChange(of: etat.derniereMesure) { derniere in
            guard let derniere = derniere else { return }
            mesure = fmt(derniere).replacingOccurrences(of: ",", with: ".")
            if !etat.dernierReglage.isEmpty { reglage = etat.dernierReglage }
            etat.derniereMesure = nil
            message = "Mesure reprise du posemètre."
        }
    }

    /* ── Noter une vue ────────────────────────────────────────────────── */

    private var carteSaisie: some View {
        Carte(
            titre: "Noter une vue",
            sousTitre: "Le sujet et le numéro de vue sont ce qui raccroche la note au négatif, une fois la planche-contact sortie. L'EV annoncé n'est utile que si tu fais l'exercice."
        ) {
            VStack(alignment: .leading, spacing: Espace.interligne) {
                LigneBoutons {
                    Deroulant(
                        intitule: "Pellicule",
                        options: PELLICULES.map { $0.nom },
                        selection: $pellicule,
                        libelle: { $0 },
                        surChoix: { nom in
                            reglages.pellicule = nom
                            if let p = PELLICULES.first(where: { $0.nom == nom }) { reglages.iso = p.iso }
                            vue = Statistiques.vueSuivante(depot.entrees, pellicule: nom)
                        }
                    )
                    Champ(valeur: $vue, intitule: "Vue", clavierDecimal: true)
                        .frame(maxWidth: 90)
                }
                Champ(valeur: $sujet, intitule: "Sujet", indication: "la Restonica au soleil rasant")
                LigneBoutons {
                    Deroulant(
                        intitule: "Scène",
                        options: TYPES_DE_SCENE,
                        selection: $type,
                        libelle: { $0 }
                    )
                    Champ(valeur: $reglage, intitule: "Réglage", indication: "f/8 · 1/500")
                }
                LigneBoutons {
                    Champ(valeur: $annonce, intitule: "EV annoncé", clavierDecimal: true)
                    Champ(valeur: $mesure, intitule: "EV mesuré", clavierDecimal: true)
                }
                Champ(valeur: $note, intitule: "Notes", indication: "filtre jaune, développement +1, à retirer")
                BoutonPlat(texte: "Enregistrer la vue", accent: true, extensible: true) {
                    enregistrer()
                }
                BandeauEtat(texte: message, alerte: message.hasPrefix("Rien"))
            }
        }
    }

    private func enregistrer() {
        let a = Double(annonce.replacingOccurrences(of: ",", with: "."))
        let m = Double(mesure.replacingOccurrences(of: ",", with: "."))
        if sujet.isEmpty && a == nil && m == nil && note.isEmpty {
            message = "Rien à enregistrer : donne au moins un sujet, une note ou une mesure."
            return
        }
        let maintenant = Date()
        /* Locale POSIX : « 2026-09-06 » doit rester « 2026-09-06 », quel que
           soit le calendrier réglé sur l'appareil. */
        let formatDate = DateFormatter()
        formatDate.locale = Locale(identifier: "en_US_POSIX")
        formatDate.dateFormat = "yyyy-MM-dd"
        let formatHeure = DateFormatter()
        formatHeure.locale = Locale(identifier: "en_US_POSIX")
        formatHeure.dateFormat = "HH:mm"

        depot.ajouter(
            EntreeJournal(
                identifiant: Int64(maintenant.timeIntervalSince1970 * 1000),
                date: formatDate.string(from: maintenant),
                heure: formatHeure.string(from: maintenant),
                pellicule: pellicule,
                vue: vue.trimmingCharacters(in: .whitespaces),
                sujet: sujet.trimmingCharacters(in: .whitespaces),
                typeScene: type,
                annonce: a,
                mesure: m,
                reglage: reglage.trimmingCharacters(in: .whitespaces),
                note: note.trimmingCharacters(in: .whitespaces)
            )
        )
        let ecart = (a != nil && m != nil) ? a! - m! : nil
        if let ecart = ecart {
            if abs(ecart) < 0.3 {
                message = "Vue \(vue) — écart \(signe(ecart)) diaph. Bien vu."
            } else if ecart > 0 {
                message = "Vue \(vue) — écart \(signe(ecart)). Tu as cru qu'il faisait plus clair qu'il ne faisait."
            } else {
                message = "Vue \(vue) — écart \(signe(ecart)). Tu as cru qu'il faisait plus sombre qu'il ne faisait."
            }
        } else {
            message = "Vue \(vue) enregistrée."
        }
        sujet = ""; annonce = ""; mesure = ""; note = ""; reglage = ""
        vue = Statistiques.vueSuivante(depot.entrees, pellicule: pellicule)
    }

    /* ── Le biais ─────────────────────────────────────────────────────── */

    private var carteBiais: some View {
        Carte(
            titre: "Ton biais",
            sousTitre: "Écart = annoncé − mesuré. Positif : tu crois qu'il fait plus clair qu'il ne fait, et tu sous-exposes."
        ) {
            HStack {
                Text(bilan.map { signe($0.biais) } ?? "—")
                    .font(Police.chiffreEnorme)
                    .foregroundColor(bilan == nil ? Teinte.contour : Teinte.surSurface)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(bilan?.verdict ?? "aucune vue annoncée puis mesurée")
                    .font(Police.corps)
                    .foregroundColor(Teinte.surVarianteSurface)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(minHeight: 64)

            if let bilan = bilan {
                Separateur()
                Ligne(
                    intitule: "Vues comptées",
                    valeur: "\(bilan.nombre)",
                    detail: bilan.nombre < 10 ? "encore un peu court" : "échantillon utile"
                )
                Ligne(intitule: "Erreur absolue moyenne", valeur: fmt(bilan.erreurAbsolue) + " diaph")
                Ligne(
                    intitule: "Dans ±½ diaph",
                    valeur: "\(arrondi(bilan.partDansDemiDiaph * 100)) %",
                    detail: "objectif : 70 %"
                )
                Ligne(
                    intitule: "Dans ±1 diaph",
                    valeur: "\(arrondi(bilan.partDansUnDiaph * 100)) %",
                    detail: "objectif : 95 %"
                )

                let parType = Statistiques.parType(depot.entrees)
                if parType.count > 1 {
                    Separateur()
                    Etiquette("Où ton œil se trompe")
                    Spacer().frame(height: 4)
                    ForEach(parType) { ligne in
                        Ligne(
                            intitule: ligne.type,
                            valeur: signe(ligne.bilan.biais),
                            detail: "\(ligne.bilan.nombre) vues · \(ligne.bilan.verdict)"
                        )
                    }
                }
            }
        }
    }

    /* ── Le carnet ────────────────────────────────────────────────────── */

    private var carteCarnet: some View {
        let n = depot.entrees.count
        let pluriel = n > 1 ? "s" : ""
        return Carte(
            titre: "Le carnet",
            sousTitre: "\(n) vue\(pluriel) notée\(pluriel)"
        ) {
            if depot.entrees.isEmpty {
                Text("Rien encore. Note une vue et elle apparaîtra ici, la plus récente en haut.")
                    .font(Police.corps)
                    .foregroundColor(Teinte.surVarianteSurface)
                    .fixedSize(horizontal: false, vertical: true)
            } else {
                let inverse = depot.entrees.reversed().map { $0 }
                let visibles = toutVoir ? inverse : Array(inverse.prefix(8))
                ForEach(visibles) { e in
                    LigneCarnet(entree: e) { depot.supprimer(identifiant: e.identifiant) }
                }
                Spacer().frame(height: Espace.interligne)
                LigneBoutons {
                    if n > 8 {
                        BoutonPlat(texte: toutVoir ? "Réduire" : "Tout voir (\(n))") {
                            toutVoir.toggle()
                        }
                    }
                    BoutonPlat(texte: "Tout effacer") { depot.vider() }
                }
            }
        }
    }
}

/// Une vue du carnet : ce qu'on a photographié, et à quoi on l'a exposé.
private struct LigneCarnet: View {

    let entree: EntreeJournal
    let surSuppression: () -> Void

    @State private var confirme = false

    private var entete: String {
        var morceaux: [String] = []
        if !entree.vue.isEmpty { morceaux.append("vue \(entree.vue)") }
        if !entree.pellicule.isEmpty { morceaux.append(entree.pellicule) }
        morceaux.append(entree.date + (entree.heure.isEmpty ? "" : " · " + entree.heure))
        return morceaux.joined(separator: " · ")
    }

    private var lignes: [String] {
        var sortie: [String] = []
        if !entree.reglage.isEmpty { sortie.append(entree.reglage) }
        if let ecart = entree.ecart, let a = entree.annonce, let m = entree.mesure {
            sortie.append("annoncé \(fmt(a)) · mesuré \(fmt(m)) · écart \(signe(ecart))")
        } else if let m = entree.mesure {
            sortie.append("mesuré \(fmt(m))")
        }
        if !entree.typeScene.isEmpty { sortie.append(entree.typeScene) }
        return sortie
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 1) {
                    Text(entree.sujet.isEmpty ? "Vue sans sujet" : entree.sujet)
                        .font(Police.corps)
                        .foregroundColor(Teinte.surSurface)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(entete)
                        .font(Police.detail)
                        .foregroundColor(Teinte.surVarianteSurface)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Button {
                    if confirme { surSuppression() } else { confirme = true }
                } label: {
                    Text(confirme ? "confirmer" : "✕")
                        .font(Police.detail)
                        .foregroundColor(confirme ? Teinte.erreur : Teinte.surVarianteSurface)
                        .padding(.leading, 10)
                        .padding(.top, 2)
                }
                .buttonStyle(.plain)
            }
            if !lignes.isEmpty {
                Spacer().frame(height: 3)
                Text(lignes.joined(separator: "  ·  "))
                    .font(Police.detail)
                    .foregroundColor(Teinte.surVarianteSurface)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if !entree.note.isEmpty {
                Spacer().frame(height: 3)
                Text(entree.note)
                    .font(Police.detail)
                    .foregroundColor(Teinte.surVarianteSurface)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.horizontal, 11)
        .padding(.vertical, 9)
        .background(
            RoundedRectangle(cornerRadius: 11, style: .continuous).fill(Teinte.varianteSurface)
        )
        .padding(.vertical, 3)
    }
}
