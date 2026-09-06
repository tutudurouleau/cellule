import SwiftUI

struct EcranEstimer: View {

    @EnvironmentObject private var reglages: Reglages
    @EnvironmentObject private var etat: EtatApplication

    @State private var ciel: EtatDuCiel = CIELS[0]
    @State private var obstacle: Obstacle = OBSTACLES[0]
    @State private var renvois: Set<String> = []
    @State private var modele: Modele = .physique
    @State private var parCalcul = true

    @State private var lieu: Lieu = LIEUX[0]
    @State private var mois: Int = Calendar.current.component(.month, from: Date())
    @State private var heure: Double = {
        let m = Calendar.current.dateComponents([.hour, .minute], from: Date())
        return Double(m.hour ?? 12) + Double(m.minute ?? 0) / 60.0
    }()
    @State private var hauteurAVue: Double = 45.0

    private var jour: Int {
        Calendrier.jourDeLAnnee(
            annee: Calendar.current.component(.year, from: Date()),
            mois: mois,
            jour: 15
        )
    }

    private var hauteur: Double {
        guard parCalcul else { return hauteurAVue }
        return Soleil.hauteur(
            latitudeNord: lieu.latitude,
            jourDeLAnnee: jour,
            tempsSolaire: Soleil.tempsSolaire(
                heureLegale: heure,
                jourDeLAnnee: jour,
                longitudeEst: lieu.longitude,
                decalageFuseau: reglages.fuseau
            )
        )
    }

    private var estimation: Estimation {
        Estimateur.calculer(
            Scene(
                ciel: ciel,
                hauteurSoleil: hauteur,
                obstacle: obstacle,
                renvois: renvois,
                iso: reglages.iso,
                modele: modele
            )
        )
    }

    /// « 17h30 » — l'heure du curseur, telle qu'on la lit.
    private var heureLisible: String {
        let heures = Int(heure)
        let minutes = Int(heure.truncatingRemainder(dividingBy: 1) * 60)
        return String(format: "%02dh%02d", heures, minutes)
    }

    /// Où en est le soleil : son midi, son culmen, et sa hauteur à cette heure.
    private var phraseSoleil: String {
        let midi = Soleil.midiSolaire(
            jourDeLAnnee: jour, longitudeEst: lieu.longitude, decalageFuseau: reglages.fuseau
        )
        let midiLisible = String(
            format: "%dh%02d",
            Int(midi),
            arrondi(midi.truncatingRemainder(dividingBy: 1) * 60)
        )
        let culmen = fmt(
            Soleil.hauteur(latitudeNord: lieu.latitude, jourDeLAnnee: jour, tempsSolaire: 12.0)
        )
        return "Midi solaire à \(midiLisible) · le soleil culmine à \(culmen)°"
            + " · à l'heure indiquée il est à \(fmt(hauteur))°."
    }

    private var vitesseVoulue: Double {
        reglages.vitesseReference > 0 ? reglages.vitesseReference : 1.0 / Double(reglages.iso)
    }

    var body: some View {
        let e = estimation
        let couple = coupleConseille(evAppareil: e.evAppareil, vitesseVoulueSec: vitesseVoulue)

        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: Espace.interligne)
                resultat(e: e, couple: couple)
                carteCiel(e: e)
                carteHauteur
                carteObstacle(e: e)
                carteRenvois(e: e)
                carteFilm(e: e)
            }
            .padding(.bottom, 16)
        }
        .background(Teinte.fond)
        .onAppear {
            lieu = LIEUX.min { abs($0.latitude - reglages.latitude) < abs($1.latitude - reglages.latitude) } ?? LIEUX[0]
            partager(e: estimation)
        }
        .onChange(of: e.ev100Scene) { _ in partager(e: estimation) }
    }

    /// Ce que le carnet reprendra si on lui verse l'estimation.
    private func partager(e: Estimation) {
        etat.derniereEstimation = e.ev100Scene
        etat.dernierTypeScene = Statistiques.typeSuggere(ev100: e.ev100Scene, obstacle: obstacle)
        let chaine = e.termes
            .filter { $0.diaphs != nil }
            .map { terme -> String in
                let valeur: String = signe(terme.diaphs ?? 0)
                let nom: String = terme.nom.lowercased()
                return "\(valeur) \(nom)"
            }
            .joined(separator: ", ")
        let debut: String = "ciel \(fmt(e.ev100Ciel))"
        let milieu: String = chaine.isEmpty ? "" : ", \(chaine)"
        etat.derniereChaine = "\(debut)\(milieu) → \(fmt(e.ev100Scene))"
    }

    private func resultat(e: Estimation, couple: Couple?) -> some View {
        Carte {
            HStack(alignment: .lastTextBaseline, spacing: 0) {
                Text(fmt(e.ev100Scene))
                    .font(Police.chiffreEnorme)
                    .foregroundColor(Teinte.surSurface)
                Text("  EV₁₀₀")
                    .font(Police.chiffreMoyen)
                    .foregroundColor(Teinte.surVarianteSurface)
            }
            if let couple = couple {
                Text("\(libelleOuverture(couple.ouverture)) · \(couple.vitesse.libelle)   à \(reglages.iso) ISO")
                    .font(.system(size: 24, weight: .semibold).monospacedDigit())
                    .foregroundColor(Teinte.primaire)
                Text("Exact \(libelleOuverture(couple.ouvertureExacte)) — l'arrondi coûte \(libelleDiaphs(couple.ecartDiaphs)).")
                    .font(Police.detail)
                    .foregroundColor(Teinte.surVarianteSurface)
            }
            Spacer().frame(height: 10)
            Rectangle().fill(Teinte.contourVariante).frame(height: 1)
            Spacer().frame(height: 6)
            ForEach(e.termes) { terme in
                Ligne(
                    intitule: terme.nom,
                    valeur: terme.ev.map { "EV " + fmt($0) } ?? signe(terme.diaphs ?? 0),
                    detail: terme.detail,
                    accent: terme.ev != nil
                )
            }
            Text(
                modele == .physique
                    ? "Les termes s'additionnent en diaphs, jamais en pourcentages — c'est tout l'intérêt du log₂."
                    : "Grille mentale : facteurs bruts, exactement ce que tu calcules de tête."
            )
            .font(Police.detail)
            .foregroundColor(Teinte.surVarianteSurface)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.top, 8)
        }
    }

    private func carteCiel(e: Estimation) -> some View {
        Carte(
            titre: "1 · La lumière du ciel",
            sousTitre: "Le repère infaillible, c'est l'ombre portée."
        ) {
            ForEach(CIELS) { c in
                let ev = modele == .physique
                    ? Ciel.eclairement(hauteurDeg: hauteur, ciel: c).ev100
                    : 15 + c.plat + Ciel.perteHauteurPlate(hauteur)
                Option(
                    nom: c.nom, detail: c.detail,
                    valeur: "EV " + fmt(ev),
                    actif: c.id == ciel.id
                ) { ciel = c }
            }
        }
    }

    private var carteHauteur: some View {
        Carte(
            titre: "2 · La hauteur du soleil",
            sousTitre: "L'éclairement suit le sinus de la hauteur, puis chute plus vite encore sous 15°, l'atmosphère traversée s'épaississant."
        ) {
            ChoixSegmente(
                options: [true, false],
                selection: $parCalcul,
                libelle: { $0 ? "Calculer" : "À vue" }
            )
            Spacer().frame(height: 6)
            if parCalcul {
                LigneBoutons {
                    Deroulant(
                        options: LIEUX,
                        selection: $lieu,
                        libelle: { $0.nom },
                        surChoix: { l in
                            reglages.latitude = l.latitude
                            reglages.longitude = l.longitude
                        }
                    )
                    Deroulant(
                        options: Array(1...12),
                        selection: $mois,
                        libelle: { moisNom($0) }
                    )
                }
                Spacer().frame(height: 6)
                Text("Heure légale : \(heureLisible)")
                    .font(Police.corps)
                    .foregroundColor(Teinte.surSurface)
                Slider(value: $heure, in: 0...23.98)
                    .tint(Teinte.primaire)
                Text(phraseSoleil)
                .font(Police.detail)
                .foregroundColor(Teinte.surVarianteSurface)
                .fixedSize(horizontal: false, vertical: true)
            } else {
                ForEach(RepereOmbre.tous) { repere in
                    let ev = Ciel.eclairement(hauteurDeg: repere.hauteur, ciel: ciel).ev100
                    Option(
                        nom: "\(Int(repere.hauteur))°", detail: repere.detail,
                        valeur: signe(ev - 15),
                        actif: hauteurAVue == repere.hauteur
                    ) { hauteurAVue = repere.hauteur }
                }
            }
        }
    }

    private func carteObstacle(e: Estimation) -> some View {
        Carte(
            titre: "3 · Ce qui bouche la lumière",
            sousTitre: "L'obstacle dominant entre le ciel et ton sujet."
        ) {
            ForEach(OBSTACLES) { o in
                let v = modele == .physique ? o.diaphs(partSoleil: e.partSoleil) : o.auSoleil
                Option(
                    nom: o.nom, detail: o.detail,
                    valeur: abs(v) < 0.05 ? "—" : signe(v),
                    actif: o.id == obstacle.id
                ) { obstacle = o }
            }
        }
    }

    private func carteRenvois(e: Estimation) -> some View {
        Carte(
            titre: "4 · Ce qui la renvoie",
            sousTitre: "Cumulable. Des gains d'éclairement réel, pas une correction de mesure."
        ) {
            let facteur = modele == .physique ? facteurRenvoi(partSoleil: e.partSoleil) : 1.0
            ForEach(RENVOIS) { r in
                Option(
                    nom: r.nom,
                    valeur: signe(r.diaphs * facteur),
                    actif: renvois.contains(r.id)
                ) {
                    if renvois.contains(r.id) { renvois.remove(r.id) } else { renvois.insert(r.id) }
                }
            }
        }
    }

    private func carteFilm(e: Estimation) -> some View {
        Carte(titre: "5 · Le film dans le dos") {
            LigneBoutons {
                Deroulant(
                    options: SENSIBILITES,
                    selection: $reglages.iso,
                    libelle: { "\($0) ISO" }
                )
                Deroulant(
                    options: [Modele.physique, Modele.grilleMentale],
                    selection: $modele,
                    libelle: { $0 == .physique ? "Physique" : "Grille mentale" }
                )
            }
            Text(
                modele == .physique
                    ? "Le modèle physique tient compte de la masse d'air et du ciel diffus : par temps couvert la hauteur du soleil compte beaucoup moins, et une ombre ne coupe que du soleil qui existe."
                    : "La grille mentale reprend les facteurs plats du document de référence. L'écart avec le modèle physique te dit où l'approximation lâche."
            )
            .font(Police.detail)
            .foregroundColor(Teinte.surVarianteSurface)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.top, 8)
            Spacer().frame(height: 8)
            BoutonPlat(texte: "Verser cette estimation au carnet", accent: true, extensible: true) {
                etat.annonceProposee = e.ev100Scene
            }
        }
    }
}


/// L'ombre portée comme goniomètre : ce qu'on lit au sol, et la hauteur qu'il vaut.
struct RepereOmbre: Identifiable {
    let hauteur: Double
    let detail: String
    var id: Double { hauteur }

    static let tous: [RepereOmbre] = [
        RepereOmbre(hauteur: 75.0, detail: "Ombre très courte, été, midi"),
        RepereOmbre(hauteur: 60.0, detail: "Ombre = ⅗ de ta taille"),
        RepereOmbre(hauteur: 45.0, detail: "Ombre = ta taille exactement"),
        RepereOmbre(hauteur: 30.0, detail: "Ombre = 2 × ta taille"),
        RepereOmbre(hauteur: 20.0, detail: "Ombre = 3 × ta taille"),
        RepereOmbre(hauteur: 10.0, detail: "Ombre = 6 × ta taille, lumière rasante"),
        RepereOmbre(hauteur: 4.0, detail: "Soleil sur l'horizon"),
        RepereOmbre(hauteur: -3.0, detail: "Crépuscule civil")
    ]
}
