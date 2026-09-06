import SwiftUI

struct EcranTables: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: Espace.interligne)
                tableMaitresse
                facteurs
                reflectances
                zoneSystem
                sensibilite
                filtres
                relations
                reserves
            }
            .padding(.bottom, 16)
        }
        .background(Teinte.fond)
    }

    private var tableMaitresse: some View {
        Carte(
            titre: "La table maîtresse — 100 ISO",
            sousTitre: "Ce qui arrive sur la scène, ce que tu affiches sur l'appareil, et à quoi ça ressemble. Sunny 16 est la ligne EV 15."
        ) {
            ForEach([16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 3, 0, -3, -6], id: \.self) { ev in
                let c = coupleDeTable(Double(ev))
                Ligne(
                    intitule: "EV \(ev)",
                    valeur: c.map { "\(libelleOuverture($0.ouverture)) · \($0.vitesse.libelle)" } ?? "—",
                    detail: Situations.nom(Double(ev)) + " · " + fmtLux(Photometrie.lux(Double(ev))),
                    accent: ev == 15
                )
            }
        }
    }

    private var facteurs: some View {
        Carte(
            titre: "Les facteurs cumulables",
            sousTitre: "Départ : plein soleil, soleil haut, terrain ouvert, sujet à 18 % → EV 15, soit f/16 au 1/ISO. Tout le reste s'ajoute en diaphs."
        ) {
            ForEach(CIELS.filter { $0.plat != 0.0 }) { c in
                Ligne(intitule: "Ciel — " + c.nom.lowercased(), valeur: signe(c.plat), detail: c.detail)
            }
            Spacer().frame(height: 4)
            ForEach(OBSTACLES.filter { $0.auSoleil != 0.0 }) { o in
                Ligne(intitule: "Obstacle — " + o.nom.lowercased(), valeur: signe(o.auSoleil), detail: o.detail)
            }
            Spacer().frame(height: 4)
            ForEach(RENVOIS) { r in
                Ligne(intitule: "Renvoi — " + r.nom.lowercased(), valeur: signe(r.diaphs))
            }
        }
    }

    private var reflectances: some View {
        Carte(
            titre: "Réflectances",
            sousTitre: "L'écart au gris 18 % vaut log₂(ρ / 0,18). C'est la correction à faire sur toute lecture réfléchie."
        ) {
            ForEach(MATIERES) { m in
                Ligne(
                    intitule: m.nom,
                    valeur: libelleDiaphs(m.ecartAuGris),
                    detail: "ρ = " + fmt(m.reflectance, 3),
                    accent: m.reflectance == 0.18
                )
            }
            Text("Attention à l'eau : sa réflectance diffuse est très basse, mais sa composante spéculaire approche 1,0. Une rivière au soleil rasant renvoie à la fois presque rien et presque tout — d'où sa dynamique énorme, et le piège qu'elle tend à toute cellule moyenne.")
                .font(Police.detail)
                .foregroundColor(Teinte.surVarianteSurface)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 8)
        }
    }

    private var zoneSystem: some View {
        Carte(
            titre: "Le zone system",
            sousTitre: "Une zone = un diaph. La zone V est ce que donne toute cellule, quelle que soit la matière visée."
        ) {
            ForEach(ZONES) { z in
                Ligne(
                    intitule: "Zone " + z.chiffre,
                    valeur: fmt(z.reflectance * 100, 1) + " %",
                    detail: z.rendu,
                    accent: z.ecart == 0
                )
            }
        }
    }

    private var sensibilite: some View {
        Carte(titre: "Sensibilité", sousTitre: "EV = EV₁₀₀ + log₂(ISO/100).") {
            ForEach(SENSIBILITES, id: \.self) { iso in
                let c = coupleDeTable(15.0 + Photometrie.decalageIso(iso))
                Ligne(
                    intitule: "\(iso) ISO",
                    valeur: signe(Photometrie.decalageIso(iso), 0),
                    detail: c.map { "Sunny 16 devient \(libelleOuverture($0.ouverture)) · \($0.vitesse.libelle)" },
                    accent: iso == 100
                )
            }
        }
    }

    private var filtres: some View {
        Carte(
            titre: "Filtres neutres",
            sousTitre: "Règle des 180° : t = 1 / (2 × fps). La vitesse étant fixée, il ne reste que l'ouverture, l'EI et les ND."
        ) {
            ForEach(FILTRES_ND.filter { $0.diaphs > 0 }, id: \.libelle) { f in
                Ligne(
                    intitule: f.libelle,
                    valeur: "\(f.diaphs) diaph" + (f.diaphs > 1 ? "s" : "")
                )
            }
            Text("25 fps, 800 EI, plein soleil : EV 18 à 1/50 demande f/64. Un ND 1,2 te ramène à f/18, un ND 1,8 à f/9.")
                .font(Police.detail)
                .foregroundColor(Teinte.surVarianteSurface)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 8)
        }
    }

    private var relations: some View {
        Carte(titre: "Les six relations") {
            ForEach(Relation.toutes) { r in
                Ligne(intitule: r.formule, detail: r.sens)
            }
            Text("Les cinq premières font le lien entre le monde et l'appareil. La sixième fait le lien entre le monde et ton œil — c'est celle qui explique pourquoi ton intuition et ton posemètre ne sont jamais tout à fait d'accord.")
                .font(Police.detail)
                .foregroundColor(Teinte.surVarianteSurface)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 8)
        }
    }

    private var reserves: some View {
        Carte(titre: "Ce que ces chiffres ne disent pas") {
            Text("""
C = 250 et K = 12,5 ne sont pas cohérentes entre elles : la cohérence exigerait C ≈ 218. Il reste 0,2 diaph d'écart structurel entre une cellule incidente et un spotmètre sur charte grise.

Le modèle atmosphérique suppose un ciel clair. Brume de chaleur, poussière, fumée : chacune retire de ½ à 2 diaphs sans prévenir, et l'œil s'y adapte trop bien pour les voir.

Les réflectances sont des médianes. De l'asphalte mouillé chute vers 0,05 ; de l'herbe sèche perd 0,10 sur de l'herbe irriguée.

Et en cas de doute avec du négatif : surexpose. L'asymétrie est réelle et large.
""")
            .font(Police.detail)
            .foregroundColor(Teinte.surVarianteSurface)
            .fixedSize(horizontal: false, vertical: true)
        }
    }
}


/// Les six relations qui font le lien entre le monde, l'appareil et l'œil.
struct Relation: Identifiable {
    let formule: String
    let sens: String
    var id: String { formule }

    static let toutes: [Relation] = [
        Relation(formule: "L = E × ρ / π", sens: "luminance depuis éclairement et matière"),
        Relation(formule: "EV₁₀₀ = log₂(E / 2,5)", sens: "éclairement incident → EV"),
        Relation(formule: "EV₁₀₀ = log₂(L / 0,125)", sens: "luminance réfléchie → EV"),
        Relation(formule: "EV = log₂(N² / t)", sens: "réglages → EV"),
        Relation(formule: "ΔEV = log₂(ρ / 0,18)", sens: "correction de réflectance"),
        Relation(formule: "L* = 116 × Y^⅓ − 16", sens: "luminance physique → clarté perçue")
    ]
}
