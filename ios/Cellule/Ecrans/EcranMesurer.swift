import AVFoundation
import SwiftUI

enum ModeMesure: String, CaseIterable, Hashable {
    case spot, moyenne, incident

    var libelle: String {
        switch self {
        case .spot: return "Spot"
        case .moyenne: return "Moyenne"
        case .incident: return "Incident"
        }
    }

    /// L'incident vise la source : c'est l'objectif frontal, comme l'écran.
    var position: AVCaptureDevice.Position {
        self == .incident ? .front : .back
    }
}

private struct Memoire: Identifiable {
    let id = UUID()
    let etiquette: String
    let ev: Double
}

/// Ce qu'on affiche, et d'où ça vient — la provenance compte autant que le chiffre.
private struct Lecture {
    var ev: Double?
    var provenance: String
    var etat: String = ""
    var alerte: Bool = false
}

struct EcranMesurer: View {

    @EnvironmentObject private var reglages: Reglages
    @EnvironmentObject private var etat: EtatApplication
    @StateObject private var moteur = MoteurCamera()

    @State private var mode: ModeMesure = .spot
    @State private var evRetenu: Double?
    @State private var memoires: [Memoire] = []
    @State private var etalonnageOuvert = false
    @State private var diagnosticVisible = false
    @State private var zoneChoisie: Zone = ZONES[5]

    private var pellicule: Pellicule {
        PELLICULES.first { $0.nom == reglages.pellicule } ?? PELLICULES[1]
    }

    /* ── La lecture, et pourquoi elle vaut ce qu'elle vaut ─────────────── */
    private var lecture: Lecture {
        if let erreur = moteur.erreur {
            return Lecture(ev: nil, provenance: "—", etat: erreur, alerte: true)
        }
        if !moteur.autorisee {
            return Lecture(ev: nil, provenance: mode.libelle.lowercased(), etat: "")
        }
        guard let exposition = moteur.exposition else {
            return Lecture(ev: nil, provenance: mode.libelle.lowercased(), etat: "En attente de la caméra…")
        }

        switch mode {
        case .incident:
            guard let cadre = moteur.cadreEntier else {
                return Lecture(ev: nil, provenance: "incident", etat: "En attente de la caméra…")
            }
            let reflechi = Posemetre.ev100Spot(exposition, cadre, etalonnage: reglages.etalonnage)
            if reflechi.isNaN {
                return Lecture(ev: nil, provenance: "incident", etat: "Cadre inexploitable.", alerte: true)
            }
            let ev = Posemetre.ev100Incident(
                ev100Reflechi: reflechi,
                coefficient: reglages.diffuseurChoisi.coefficient
            ) + reglages.etalonnageIncident
            let d = reglages.diffuseurChoisi
            if !cadre.fiable {
                return Lecture(
                    ev: ev, provenance: "incident · \(d.nom.lowercased())",
                    etat: "Cadre cramé ou bouché — le diffuseur n'est pas en place, ou la source est trop forte.",
                    alerte: true
                )
            }
            if !moteur.aeStable && !moteur.gele {
                return Lecture(ev: ev, provenance: "incident · \(d.nom.lowercased())", etat: "L'exposition se cale…")
            }
            return Lecture(ev: ev, provenance: "incident · \(d.nom.lowercased())")

        case .moyenne:
            return Lecture(
                ev: Posemetre.ev100Moyen(exposition, etalonnage: reglages.etalonnage),
                provenance: "moyenne du cadre",
                etat: (!moteur.aeStable && !moteur.gele) ? "L'exposition se cale…" : ""
            )

        case .spot:
            let brut = moteur.spot.map {
                Posemetre.ev100Spot(exposition, $0, etalonnage: reglages.etalonnage)
            } ?? Double.nan
            if brut.isNaN {
                return Lecture(
                    ev: Posemetre.ev100Moyen(exposition, etalonnage: reglages.etalonnage),
                    provenance: "moyenne — spot indisponible",
                    etat: "Le disque ne rend rien d'exploitable : c'est la moyenne du cadre qui s'affiche.",
                    alerte: true
                )
            }
            if let spot = moteur.spot, !spot.fiable {
                return Lecture(
                    ev: brut, provenance: "spot",
                    etat: "Disque cramé ou bouché — élargis-le ou vise ailleurs.", alerte: true
                )
            }
            if !moteur.aeStable && !moteur.gele {
                return Lecture(ev: brut, provenance: "spot", etat: "L'exposition se cale…")
            }
            return Lecture(ev: brut, provenance: "spot")
        }
    }

    private var vitesseVoulue: Double {
        reglages.vitesseReference > 0 ? reglages.vitesseReference : 1.0 / Double(reglages.iso)
    }

    var body: some View {
        let lecture = self.lecture
        let ev100 = lecture.ev
        let evExposition = evRetenu ?? ev100
        let couple = evExposition.flatMap {
            coupleConseille(
                evAppareil: $0 + Photometrie.decalageIso(reglages.iso),
                vitesseVoulueSec: vitesseVoulue
            )
        }
        let ancre = evRetenu ?? ev100

        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: Espace.interligne)

                ChoixSegmente(
                    options: ModeMesure.allCases,
                    selection: $mode,
                    libelle: { $0.libelle }
                )
                .padding(.horizontal, Espace.gouttiere)

                apercu

                instrument(lecture: lecture, ev100: ev100, couple: couple)
                cartePellicule
                if let ev100 = ev100 { carteZones(ev100: ev100, ancre: ancre) }
                if mode == .incident { carteDiffuseur }
                if moteur.autorisee { carteDiagnostic }
            }
            .padding(.bottom, 20)
        }
        .background(Teinte.fond)
        .onAppear {
            moteur.plageVideo = reglages.plageVideo
            moteur.demanderAutorisation()
            moteur.demarrer(position: mode.position)
        }
        .onDisappear { moteur.arreter() }
        .onChange(of: mode) { nouveau in moteur.demarrer(position: nouveau.position) }
        .onChange(of: moteur.autorisee) { accordee in
            if accordee { moteur.demarrer(position: mode.position) }
        }
        .onChange(of: reglages.plageVideo) { nouvelle in moteur.plageVideo = nouvelle }
        .sheet(isPresented: $etalonnageOuvert) {
            FeuilleEtalonnage(
                moteur: moteur,
                mode: mode,
                lectureCourante: lecture.ev
            )
            .environmentObject(reglages)
        }
    }

    /* ── Aperçu ───────────────────────────────────────────────────────── */

    @ViewBuilder private var apercu: some View {
        if !moteur.autorisee {
            Carte(
                titre: "Accès à la caméra",
                sousTitre: "La mesure lit les métadonnées d'exposition : temps de pose, sensibilité, ouverture. Aucune image n'est enregistrée ni transmise."
            ) {
                BoutonPlat(texte: "Autoriser la caméra", accent: true, extensible: true) {
                    moteur.demanderAutorisation()
                }
            }
        } else {
            VStack(spacing: 0) {
                GeometryReader { geo in
                    ZStack {
                        VueApercu(session: moteur.session)
                        if mode == .spot {
                            Circle()
                                .stroke(Color.black.opacity(0.55), lineWidth: 5)
                                .frame(
                                    width: CGFloat(moteur.rayon) * 2 * min(geo.size.width, geo.size.height),
                                    height: CGFloat(moteur.rayon) * 2 * min(geo.size.width, geo.size.height)
                                )
                                .position(
                                    x: CGFloat(moteur.cibleU) * geo.size.width,
                                    y: CGFloat(moteur.cibleV) * geo.size.height
                                )
                            Circle()
                                .stroke(Teinte.primaire, lineWidth: 2.5)
                                .frame(
                                    width: CGFloat(moteur.rayon) * 2 * min(geo.size.width, geo.size.height),
                                    height: CGFloat(moteur.rayon) * 2 * min(geo.size.width, geo.size.height)
                                )
                                .position(
                                    x: CGFloat(moteur.cibleU) * geo.size.width,
                                    y: CGFloat(moteur.cibleV) * geo.size.height
                                )
                            Circle()
                                .fill(Teinte.primaire)
                                .frame(width: 5, height: 5)
                                .position(
                                    x: CGFloat(moteur.cibleU) * geo.size.width,
                                    y: CGFloat(moteur.cibleV) * geo.size.height
                                )
                        }
                    }
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { valeur in
                                guard mode == .spot else { return }
                                moteur.viser(
                                    u: Float(valeur.location.x / max(geo.size.width, 1)),
                                    v: Float(valeur.location.y / max(geo.size.height, 1))
                                )
                            }
                    )
                }
                /* Les proportions viennent de l'image elle-même : si le cadre
                   ne les respectait pas, l'image y serait mise en boîte aux
                   lettres et le point touché ne désignerait plus le pixel visé. */
                .aspectRatio(moteur.rapportApercu, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .padding(.horizontal, Espace.gouttiere)
                .padding(.vertical, 5)

                if mode == .spot {
                    HStack {
                        Etiquette("Disque")
                        Slider(
                            value: Binding(
                                get: { Double(moteur.rayon) },
                                set: { moteur.reglerRayon(Float($0)) }
                            ),
                            in: 0.02...0.25
                        )
                        .tint(Teinte.primaire)
                        .padding(.leading, 12)
                    }
                    .padding(.horizontal, Espace.gouttiere + 4)
                }
            }
        }
    }

    /* ── L'instrument ─────────────────────────────────────────────────── */

    private func instrument(lecture: Lecture, ev100: Double?, couple: Couple?) -> some View {
        Carte {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 0) {
                    HStack(alignment: .lastTextBaseline, spacing: 0) {
                        Text(ev100 != nil ? fmt(ev100!) : "––,–")
                            .font(Police.chiffreEnorme)
                            .foregroundColor(ev100 != nil ? Teinte.surSurface : Teinte.contour)
                        Text(" EV")
                            .font(Police.corps)
                            .foregroundColor(Teinte.surVarianteSurface)
                    }
                    Text(lecture.provenance)
                        .font(Police.detail)
                        .lineLimit(1)
                        .foregroundColor(Teinte.surVarianteSurface)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 0) {
                    Text(couple.map { libelleOuverture($0.ouverture) } ?? "f/—")
                        .font(Police.chiffreGrand)
                        .foregroundColor(Teinte.primaire)
                    Text(couple?.vitesse.libelle ?? "—")
                        .font(Police.chiffreGrand)
                        .foregroundColor(Teinte.primaire)
                    Text("\(reglages.iso) ISO")
                        .font(Police.detail)
                        .foregroundColor(Teinte.surVarianteSurface)
                }
            }
            .frame(height: 88)

            Spacer().frame(height: Espace.interligne)
            BandeauEtat(texte: lecture.etat, alerte: lecture.alerte)

            Spacer().frame(height: Espace.interligne)
            /* Trois lignes fixes : elles changent de contenu, jamais de place. */
            Ligne(
                intitule: "Niveau",
                valeur: ev100.map { fmtLux(Photometrie.lux($0)) } ?? "—",
                detail: ev100.map { Situations.nom($0) } ?? " "
            )
            Ligne(
                intitule: "Diaph exact",
                valeur: couple.map { libelleOuverture($0.ouvertureExacte) } ?? "—",
                detail: couple.map { "l'arrondi au diaph plein coûte \(libelleDiaphs($0.ecartDiaphs))" } ?? " "
            )
            Ligne(
                intitule: "L'appareil expose à",
                valeur: moteur.exposition.map { "\(libelleOuverture($0.ouverture)) · \($0.iso) ISO" } ?? "—",
                detail: moteur.exposition.map {
                    fmt($0.tempsPoseSec * 1000, 2) + " ms"
                        + (reglages.etalonnage != 0 ? "  ·  étalonnage \(signe(reglages.etalonnage, 2))" : "")
                } ?? " "
            )

            Spacer().frame(height: Espace.interligne)
            LigneBoutons {
                BoutonPlat(texte: moteur.gele ? "Libérer" : "Figer", accent: moteur.gele) {
                    moteur.basculerGel()
                }
                BoutonPlat(texte: "Étalonner", actif: moteur.exposition != nil) {
                    etalonnageOuvert = true
                }
                if let ev100 = ev100 {
                    BoutonPlat(texte: "Au carnet", accent: true, extensible: true) {
                        etat.derniereMesure = ev100
                        etat.dernierReglage = couple.map {
                            "\(libelleOuverture($0.ouverture)) · \($0.vitesse.libelle)"
                        } ?? ""
                    }
                }
            }
        }
    }

    /* ── Pellicule chargée ────────────────────────────────────────────── */

    private var cartePellicule: some View {
        Carte(titre: "Pellicule") {
            LigneBoutons {
                Deroulant(
                    intitule: "Chargée",
                    options: PELLICULES,
                    selection: Binding(
                        get: { pellicule },
                        set: { reglages.pellicule = $0.nom; reglages.iso = $0.iso }
                    ),
                    libelle: { $0.nom }
                )
                Deroulant(
                    intitule: "Exposée à",
                    options: SENSIBILITES,
                    selection: $reglages.iso,
                    libelle: { "\($0) ISO" }
                )
                .frame(maxWidth: 130)
            }
            Spacer().frame(height: Espace.interligne)
            Deroulant(
                intitule: "Vitesse de référence",
                options: [0.0, 1 / 1000.0, 1 / 500.0, 1 / 250.0, 1 / 125.0, 1 / 60.0, 1 / 50.0, 1 / 30.0],
                selection: $reglages.vitesseReference,
                libelle: { v in
                    if v == 0 { return "1/ISO" }
                    if abs(v - 1 / 50.0) < 1e-9 { return "1/50 ciné" }
                    return "1/" + String(arrondi(1 / v))
                }
            )
            Spacer().frame(height: Espace.interligne)
            Text(conseilLatitude(pellicule.type))
                .font(Police.detail)
                .foregroundColor(Teinte.surVarianteSurface)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    /* ── Zones ────────────────────────────────────────────────────────── */

    private func carteZones(ev100: Double, ancre: Double?) -> some View {
        Carte(
            titre: "Zones",
            sousTitre: "Place l'ombre la plus sombre où tu veux encore de la matière en zone III, et le reste tombe où il tombe."
        ) {
            if let ancre = ancre {
                let z = Zones.zoneLaPlusProche(5 + (ev100 - ancre))
                Ligne(
                    intitule: "Lecture en cours",
                    valeur: "zone \(z.chiffre)",
                    detail: z.rendu,
                    accent: true
                )
            }
            if let evRetenu = evRetenu {
                Ligne(
                    intitule: "Exposition retenue",
                    valeur: "EV " + fmt(evRetenu),
                    detail: "tout s'y rapporte"
                )
            }
            Spacer().frame(height: Espace.interligne)
            LigneBoutons {
                Deroulant(
                    intitule: "Placer la lecture en",
                    options: ZONES,
                    selection: $zoneChoisie,
                    libelle: { "zone \($0.chiffre)" },
                    surChoix: { zone in
                        evRetenu = Zones.expositionPourPlacer(ev100Lu: ev100, zone: zone)
                    }
                )
                if evRetenu != nil {
                    BoutonPlat(texte: "Libérer") { evRetenu = nil }
                }
            }
            Separateur()
            LigneBoutons {
                BoutonPlat(texte: "Mémoriser cette lecture", extensible: true) {
                    memoires.append(Memoire(etiquette: "Lecture \(memoires.count + 1)", ev: ev100))
                }
                if !memoires.isEmpty {
                    BoutonPlat(texte: "Effacer") { memoires = [] }
                }
            }
            ForEach(memoires) { m in
                let z = ancre.map { 5 + (m.ev - $0) }
                Ligne(
                    intitule: m.etiquette,
                    valeur: "EV " + fmt(m.ev),
                    detail: z.map { "zone \(Zones.zoneLaPlusProche($0).chiffre)" }
                )
            }
            if memoires.count >= 2 {
                let amplitude = (memoires.map { $0.ev }.max() ?? 0) - (memoires.map { $0.ev }.min() ?? 0)
                Spacer().frame(height: 4)
                Text(
                    "Amplitude : \(fmt(amplitude)) diaphs. " + {
                        if amplitude <= 5 { return "Tout tient, même en inversible." }
                        if amplitude <= 9 { return "Le négatif encaisse sans broncher." }
                        return "Au-delà de la plage du support : tu ne rates pas l'exposition, tu choisis ce que tu sacrifies."
                    }()
                )
                .font(Police.detail)
                .foregroundColor(Teinte.surVarianteSurface)
                .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    /* ── Le diffuseur, propre à la voie incidente d'iOS ───────────────── */

    private var carteDiffuseur: some View {
        Carte(
            titre: "Le diffuseur",
            sousTitre: "iOS n'ouvre aucune API au capteur de luminosité ambiante. La voie incidente passe donc par l'objectif frontal — celui qui regarde là où regarde l'écran — et un diffuseur dont on retire le coefficient."
        ) {
            Deroulant(
                intitule: "En place",
                options: DIFFUSEURS,
                selection: Binding(
                    get: { reglages.diffuseurChoisi },
                    set: { reglages.diffuseur = $0.id }
                ),
                libelle: { $0.nom }
            )
            Spacer().frame(height: Espace.interligne)
            Ligne(
                intitule: "Coefficient retiré",
                valeur: libelleDiaphs(-Zones.correctionReflectance(reglages.diffuseurChoisi.coefficient)),
                detail: reglages.diffuseurChoisi.detail
                    + " · ρ = " + fmt(reglages.diffuseurChoisi.coefficient, 2)
            )
            if reglages.etalonnageIncident != 0 {
                Ligne(
                    intitule: "Étalonnage incident",
                    valeur: signe(reglages.etalonnageIncident, 2),
                    detail: "mesuré contre une cellule de confiance"
                )
            }
            Spacer().frame(height: Espace.interligne)
            Text("Tourne l'écran vers la source, comme on présente le dôme d'une cellule. Sans diffuseur devant l'objectif, ce n'est pas une mesure incidente mais une lecture réfléchie de ce qui est en face.")
                .font(Police.detail)
                .foregroundColor(Teinte.surVarianteSurface)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    /* ── Diagnostic ───────────────────────────────────────────────────── */

    private var carteDiagnostic: some View {
        Carte {
            HStack {
                Etiquette("Diagnostic")
                Spacer()
                BoutonPlat(texte: diagnosticVisible ? "Masquer" : "Afficher") {
                    diagnosticVisible.toggle()
                }
            }
            if diagnosticVisible {
                Spacer().frame(height: Espace.interligne)
                if let d = moteur.diagnostic {
                    Ligne(
                        intitule: "Image analysée",
                        valeur: "\(d.largeur) × \(d.hauteur)",
                        detail: "rotation \(d.rotation)°"
                    )
                    Ligne(
                        intitule: "Pas de ligne / pixel",
                        valeur: "\(d.pasDeLigne) / \(d.pasDePixel)",
                        detail: "\(d.octets) octets lus"
                    )
                    Ligne(
                        intitule: "Visée à l'écran",
                        valeur: "\(fmt(Double(moteur.cibleU), 2)) · \(fmt(Double(moteur.cibleV), 2))",
                        detail: "sur le capteur : \(fmt(Double(d.cibleCapteurX), 2)) · \(fmt(Double(d.cibleCapteurY), 2))"
                    )
                    Ligne(
                        intitule: "Échantillons du disque",
                        valeur: "\(d.echantillons)",
                        detail: moteur.spot.map {
                            "moyenne linéaire \(fmt($0.moyenneLineaire, 4)) · "
                                + "cramés \(arrondi($0.fractionCramee * 100)) % · "
                                + "bouchés \(arrondi($0.fractionBouchee * 100)) %"
                        }
                    )
                    Ligne(
                        intitule: "Proportions de l'aperçu",
                        valeur: fmt(Double(moteur.rapportApercu), 3)
                    )
                    Ligne(
                        intitule: "Métadonnées",
                        valeur: d.sourceMetadonnees,
                        detail: moteur.aeStable ? "exposition calée" : "exposition en recherche"
                    )
                    Ligne(
                        intitule: "Plan de luminance",
                        valeur: reglages.plageVideo ? "16-235" : "0-255",
                        detail: "le format demandé à la capture, pas une supposition"
                    )
                } else {
                    Text("Aucune image analysée pour l'instant.")
                        .font(Police.detail)
                        .foregroundColor(Teinte.surVarianteSurface)
                }
            }
        }
    }
}

/* ── Étalonnage ───────────────────────────────────────────────────────── */

private struct FeuilleEtalonnage: View {

    @ObservedObject var moteur: MoteurCamera
    let mode: ModeMesure
    let lectureCourante: Double?

    @EnvironmentObject private var reglages: Reglages
    @Environment(\.dismiss) private var fermer

    @State private var matiere: Matiere = MATIERES.first { $0.reflectance == 0.18 } ?? MATIERES[8]
    @State private var reference: String = ""

    private var incident: Bool { mode == .incident }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Carte {
                        Text(
                            incident
                                ? "Présente le diffuseur à une lumière dont tu connais la valeur et indique ce qu'annonce une cellule en qui tu as confiance. Le décalage sera retiré de toutes les lectures incidentes."
                                : "Vise une surface dont tu connais la nature et indique ce qu'annonce une cellule en qui tu as confiance. Le décalage sera retiré de toutes les lectures réfléchies."
                        )
                        .font(Police.detail)
                        .foregroundColor(Teinte.surVarianteSurface)
                        .fixedSize(horizontal: false, vertical: true)

                        Spacer().frame(height: Espace.interligne)
                        if !incident {
                            Deroulant(
                                intitule: "Matière visée",
                                options: MATIERES,
                                selection: $matiere,
                                libelle: { $0.nom }
                            )
                            Spacer().frame(height: Espace.interligne)
                        }
                        Champ(
                            valeur: $reference,
                            intitule: "EV₁₀₀ de référence",
                            indication: "15,0",
                            clavierDecimal: true
                        )
                        Spacer().frame(height: Espace.interligne)
                        Ligne(
                            intitule: "Lecture en cours",
                            valeur: lectureCourante.map { "EV " + fmt($0) } ?? "—",
                            detail: incident
                                ? "étalonnage incident actuel : \(signe(reglages.etalonnageIncident, 2))"
                                : "étalonnage actuel : \(signe(reglages.etalonnage, 2))"
                        )
                    }

                    if !incident {
                        Carte(titre: "Plan de luminance") {
                            Toggle(isOn: $reglages.plageVideo) {
                                Text("Plage vidéo (16-235)")
                                    .font(Police.corps)
                                    .foregroundColor(Teinte.surSurface)
                            }
                            .tint(Teinte.primaire)
                            Spacer().frame(height: 6)
                            Text("Sur iOS ce réglage ne devine rien : il choisit le format de pixel demandé à la caméra, et le décodeur suit. Le laisser sur la plage vidéo convient à presque tout.")
                                .font(Police.detail)
                                .foregroundColor(Teinte.surVarianteSurface)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }

                    Carte {
                        LigneBoutons {
                            BoutonPlat(texte: "Réinitialiser") {
                                if incident { reglages.etalonnageIncident = 0 } else { reglages.etalonnage = 0 }
                                fermer()
                            }
                            BoutonPlat(
                                texte: "Enregistrer",
                                actif: moteur.exposition != nil,
                                accent: true,
                                extensible: true
                            ) {
                                enregistrer()
                                fermer()
                            }
                        }
                    }
                }
                .padding(.vertical, Espace.interligne)
            }
            .background(Teinte.fond)
            .navigationTitle("Étalonner")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { fermer() }
                }
            }
        }
    }

    private func enregistrer() {
        guard let cible = Double(reference.replacingOccurrences(of: ",", with: ".")),
              let exposition = moteur.exposition else { return }
        if incident {
            guard let cadre = moteur.cadreEntier else { return }
            let reflechi = Posemetre.ev100Spot(exposition, cadre, etalonnage: reglages.etalonnage)
            reglages.etalonnageIncident = Posemetre.etalonnageIncidentDepuis(
                ev100Reflechi: reflechi,
                coefficient: reglages.diffuseurChoisi.coefficient,
                ev100Vrai: cible
            )
        } else {
            guard let spot = moteur.spot else { return }
            reglages.etalonnage = Posemetre.etalonnageDepuis(
                exposition, spot, ev100Vrai: cible, reflectanceVisee: matiere.reflectance
            )
        }
    }
}
