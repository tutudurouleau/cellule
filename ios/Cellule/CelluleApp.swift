import SwiftUI

@main
struct CelluleApp: App {

    @StateObject private var reglages = Reglages()
    @StateObject private var depot = DepotJournal()
    @StateObject private var etat = EtatApplication()

    /* `Scene` est aussi le nom de la structure de scène photographique du
       cœur de calcul, qui prime dans ce module. On qualifie donc le protocole
       de SwiftUI, plutôt que de renommer un type partagé avec le portage Kotlin. */
    var body: some SwiftUI.Scene {
        WindowGroup {
            Application()
                .environmentObject(reglages)
                .environmentObject(depot)
                .environmentObject(etat)
                .preferredColorScheme(nil)
        }
    }
}

private enum Onglet: String, CaseIterable, Identifiable {
    case mesurer, estimer, carnet, tables
    var id: String { rawValue }

    var titre: String {
        switch self {
        case .mesurer: return "Mesurer"
        case .estimer: return "Estimer"
        case .carnet: return "Carnet"
        case .tables: return "Tables"
        }
    }

    var sousTitre: String {
        switch self {
        case .mesurer: return "posemètre réfléchi et incident"
        case .estimer: return "la chaîne de facteurs, sans cellule"
        case .carnet: return "ce que tu as photographié, et ton biais"
        case .tables: return "les repères à retenir"
        }
    }

    var icone: String {
        switch self {
        case .mesurer: return "viewfinder"
        case .estimer: return "sun.max"
        case .carnet: return "book.closed"
        case .tables: return "list.bullet.rectangle"
        }
    }
}

struct Application: View {

    @State private var onglet: Onglet = .mesurer

    var body: some View {
        VStack(spacing: 0) {
            Entete(onglet: onglet)
            TabView(selection: $onglet) {
                EcranMesurer()
                    .tabItem { Label(Onglet.mesurer.titre, systemImage: Onglet.mesurer.icone) }
                    .tag(Onglet.mesurer)
                EcranEstimer()
                    .tabItem { Label(Onglet.estimer.titre, systemImage: Onglet.estimer.icone) }
                    .tag(Onglet.estimer)
                EcranJournal()
                    .tabItem { Label(Onglet.carnet.titre, systemImage: Onglet.carnet.icone) }
                    .tag(Onglet.carnet)
                EcranTables()
                    .tabItem { Label(Onglet.tables.titre, systemImage: Onglet.tables.icone) }
                    .tag(Onglet.tables)
            }
            .tint(Teinte.primaire)
        }
        .background(Teinte.fond.ignoresSafeArea())
    }
}

/// L'en-tête : ce qu'on regarde, et pourquoi.
private struct Entete: View {
    let onglet: Onglet

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(onglet.titre)
                        .font(Police.chiffreGrand)
                        .foregroundColor(Teinte.surSurface)
                    Text(onglet.sousTitre)
                        .font(Police.detail)
                        .foregroundColor(Teinte.surVarianteSurface)
                }
                Spacer()
                Text("CELLULE")
                    .font(Police.etiquette)
                    .tracking(0.9)
                    .foregroundColor(Teinte.primaire)
            }
            .padding(.horizontal, Espace.gouttiere)
            .padding(.top, 10)
            .padding(.bottom, 10)
            Rectangle()
                .fill(Teinte.contourVariante)
                .frame(height: 1)
        }
        .background(Teinte.fond)
    }
}
