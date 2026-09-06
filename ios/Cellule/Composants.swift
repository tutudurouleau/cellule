import SwiftUI

/* ── Mise en forme des nombres ─────────────────────────────────────────── */

private let localeFr = Locale(identifier: "fr_FR")

func fmt(_ x: Double, _ decimales: Int = 1) -> String {
    if x.isNaN || x.isInfinite { return "—" }
    return String(format: "%.\(decimales)f", locale: localeFr, x)
}

func signe(_ x: Double, _ decimales: Int = 1) -> String {
    if x.isNaN { return "—" }
    if abs(x) < 0.05 { return "0" }
    return x > 0 ? "+" + fmt(x, decimales) : "−" + fmt(abs(x), decimales)
}

/// Groupe les milliers par une espace insécable, comme le fait le français.
private func groupeMilliers(_ x: Double) -> String {
    let f = NumberFormatter()
    f.locale = localeFr
    f.numberStyle = .decimal
    f.maximumFractionDigits = 0
    f.groupingSeparator = "\u{00A0}"
    f.usesGroupingSeparator = true
    return f.string(from: NSNumber(value: x)) ?? String(Int(x))
}

/**
 Trois chiffres significatifs : au-delà on ferait croire à une précision
 que ni l'atmosphère ni l'œil ne possèdent.
 */
func fmtLux(_ lux: Double) -> String {
    if lux.isNaN || lux.isInfinite { return "—" }
    if lux >= 1000 {
        let puissance = pow(10.0, 2 - floor(log10(lux)))
        let valeur = Double(arrondi(lux * puissance)) / puissance
        return groupeMilliers(valeur) + " lx"
    }
    if lux >= 100 { return "\(arrondi(lux)) lx" }
    if lux >= 10 { return fmt(lux, 1) + " lx" }
    if lux >= 1 { return fmt(lux, 2) + " lx" }
    if lux >= 0.01 { return fmt(lux, 3) + " lx" }
    return fmt(lux, 4) + " lx"
}

func fmtCdm2(_ l: Double) -> String {
    if l.isNaN || l.isInfinite { return "—" }
    if l >= 100 { return "\(arrondi(l)) cd/m²" }
    if l >= 1 { return fmt(l, 1) + " cd/m²" }
    return fmt(l, 3) + " cd/m²"
}

func moisNom(_ m: Int) -> String {
    [
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre"
    ][(m - 1).borne(0, 11)]
}

/* ── Blocs ─────────────────────────────────────────────────────────────── */

struct Etiquette: View {
    let texte: String
    init(_ texte: String) { self.texte = texte }

    var body: some View {
        Text(texte.uppercased(with: localeFr))
            .font(Police.etiquette)
            .tracking(0.9)
            .foregroundColor(Teinte.surVarianteSurface)
    }
}

/// Une carte : bordure fine plutôt qu'une ombre, pour ne pas alourdir.
struct Carte<Contenu: View>: View {
    private let titre: String?
    private let sousTitre: String?
    private let contenu: Contenu

    init(
        titre: String? = nil,
        sousTitre: String? = nil,
        @ViewBuilder contenu: () -> Contenu
    ) {
        self.titre = titre
        self.sousTitre = sousTitre
        self.contenu = contenu()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let titre = titre {
                Text(titre)
                    .font(Police.titreCarte)
                    .foregroundColor(Teinte.surSurface)
            }
            if let sousTitre = sousTitre {
                Spacer().frame(height: 3)
                Text(sousTitre)
                    .font(Police.detail)
                    .foregroundColor(Teinte.surVarianteSurface)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if titre != nil || sousTitre != nil {
                Spacer().frame(height: Espace.interligne)
            }
            contenu
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Espace.gouttiere)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Teinte.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Teinte.contourVariante, lineWidth: 1)
        )
        .padding(.horizontal, Espace.gouttiere)
        .padding(.vertical, 5)
    }
}

/// Une ligne « intitulé · valeur », le détail sous l'intitulé.
struct Ligne: View {
    let intitule: String
    var valeur: String = ""
    var detail: String? = nil
    var accent: Bool = false

    var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 1) {
                Text(intitule)
                    .font(Police.corps)
                    .foregroundColor(Teinte.surSurface)
                if let detail = detail, !detail.trimmingCharacters(in: .whitespaces).isEmpty {
                    Text(detail)
                        .font(Police.detail)
                        .foregroundColor(Teinte.surVarianteSurface)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if !valeur.isEmpty {
                Text(valeur)
                    .font(Police.corps)
                    .multilineTextAlignment(.trailing)
                    .foregroundColor(accent ? Teinte.primaire : Teinte.surSurface)
                    .padding(.leading, 12)
            }
        }
        .padding(.vertical, 5)
    }
}

struct Separateur: View {
    var body: some View {
        Rectangle()
            .fill(Teinte.contourVariante)
            .frame(height: 1)
            .padding(.vertical, Espace.interligne)
    }
}

/// Un choix parmi quelques-uns, en bandeau.
struct ChoixSegmente<T: Hashable>: View {
    let options: [T]
    @Binding var selection: T
    let libelle: (T) -> String

    var body: some View {
        HStack(spacing: 3) {
            ForEach(options, id: \.self) { option in
                let actif = option == selection
                Button {
                    selection = option
                } label: {
                    Text(libelle(option))
                        .font(Police.corps)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                        .foregroundColor(actif ? Teinte.primaire : Teinte.surVarianteSurface)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 9)
                        .background(
                            RoundedRectangle(cornerRadius: 9, style: .continuous)
                                .fill(actif ? Teinte.surface : Color.clear)
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(
            RoundedRectangle(cornerRadius: 11, style: .continuous)
                .fill(Teinte.varianteSurface)
        )
    }
}

/// Liste déroulante compacte.
struct Deroulant<T: Hashable>: View {
    var intitule: String = ""
    let options: [T]
    @Binding var selection: T
    let libelle: (T) -> String
    var surChoix: ((T) -> Void)? = nil

    var body: some View {
        Menu {
            ForEach(options, id: \.self) { option in
                Button(libelle(option)) {
                    selection = option
                    surChoix?(option)
                }
            }
        } label: {
            VStack(alignment: .leading, spacing: 2) {
                if !intitule.isEmpty {
                    Etiquette(intitule)
                }
                Text(libelle(selection) + "  ▾")
                    .font(Police.corps)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .foregroundColor(Teinte.surSurface)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 12)
            .padding(.vertical, 9)
            .background(
                RoundedRectangle(cornerRadius: 11, style: .continuous)
                    .fill(Teinte.varianteSurface)
            )
        }
    }
}

/// Champ de saisie, à la même hauteur que les déroulants.
struct Champ: View {
    @Binding var valeur: String
    let intitule: String
    var indication: String = ""
    var clavierDecimal: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Etiquette(intitule)
            TextField(indication, text: $valeur)
                .font(Police.corps)
                .foregroundColor(Teinte.surSurface)
                .textInputAutocapitalization(.sentences)
                .keyboardType(clavierDecimal ? .numbersAndPunctuation : .default)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 9)
        .background(
            RoundedRectangle(cornerRadius: 11, style: .continuous)
                .fill(Teinte.varianteSurface)
        )
    }
}

/**
 Bandeau d'état, de hauteur constante.

 Il occupe la même place qu'il ait quelque chose à dire ou non : c'est ce qui
 empêche l'apparition d'un avertissement de faire sauter tout ce qui suit.
 */
struct BandeauEtat: View {
    let texte: String
    var alerte: Bool = false

    var body: some View {
        HStack {
            Text(texte)
                .font(Police.detail)
                .lineLimit(2)
                .foregroundColor(alerte ? Teinte.erreur : Teinte.surVarianteSurface)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .frame(minHeight: 34, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(texte.isEmpty ? Color.clear : Teinte.varianteSurface)
        )
    }
}

/// Une pastille de valeur, pour les états courts.
struct Pastille: View {
    let texte: String
    var accent: Bool = false

    var body: some View {
        Text(texte)
            .font(Police.detail)
            .foregroundColor(accent ? Teinte.primaire : Teinte.surVarianteSurface)
            .padding(.horizontal, 9)
            .padding(.vertical, 3)
            .background(
                Capsule().fill(accent ? Teinte.conteneurPrimaire : Teinte.varianteSurface)
            )
    }
}

/// Bouton discret, aligné sur le reste.
struct BoutonPlat: View {
    let texte: String
    var actif: Bool = true
    var accent: Bool = false
    var extensible: Bool = false
    let surClic: () -> Void

    private var couleurFond: Color {
        if !actif { return Teinte.varianteSurface }
        return accent ? Teinte.conteneurPrimaire : Teinte.varianteSurface
    }

    private var couleurTexte: Color {
        if !actif { return Teinte.contour }
        return accent ? Teinte.primaire : Teinte.surSurface
    }

    var body: some View {
        Button(action: { if actif { surClic() } }) {
            Text(texte)
                .font(Police.corps)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .foregroundColor(couleurTexte)
                .frame(maxWidth: extensible ? .infinity : nil)
                .padding(.horizontal, 15)
                .padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 11, style: .continuous).fill(couleurFond)
                )
        }
        .buttonStyle(.plain)
        .disabled(!actif)
    }
}

struct LigneBoutons<Contenu: View>: View {
    private let contenu: Contenu

    init(@ViewBuilder contenu: () -> Contenu) {
        self.contenu = contenu()
    }

    var body: some View {
        HStack(spacing: Espace.interligne) { contenu }
    }
}

/// Une ligne d'option sélectionnable, avec sa valeur en diaphs à droite.
struct Option: View {
    let nom: String
    var detail: String = ""
    let valeur: String
    let actif: Bool
    let surClic: () -> Void

    var body: some View {
        Button(action: surClic) {
            HStack {
                VStack(alignment: .leading, spacing: 1) {
                    Text(nom)
                        .font(.system(size: 15, weight: actif ? .medium : .regular))
                        .foregroundColor(Teinte.surSurface)
                    if !detail.isEmpty {
                        Text(detail)
                            .font(Police.detail)
                            .foregroundColor(Teinte.surVarianteSurface)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Text(valeur)
                    .font(Police.detail)
                    .foregroundColor(actif ? Teinte.primaire : Teinte.surVarianteSurface)
            }
            .padding(.horizontal, 11)
            .padding(.vertical, 9)
            .background(
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .fill(actif ? Teinte.conteneurPrimaire : Teinte.varianteSurface)
            )
        }
        .buttonStyle(.plain)
        .padding(.vertical, 2)
    }
}
