// swift-tools-version: 5.9
import PackageDescription

/*
 Le module de photométrie, isolé de tout ce qui est iOS — exactement comme
 `core/` l'est d'Android. Il se teste donc sans appareil, sans simulateur et
 sans Xcode : `swift test` suffit, y compris en intégration continue.

 Les mêmes fichiers sont compilés dans l'application par le projet Xcode, qui
 les prend directement dans `Sources/CelluleCore` : une seule copie, jamais
 deux versions à tenir d'accord.
 */
let package = Package(
    name: "Cellule",
    platforms: [.macOS(.v12), .iOS(.v16)],
    products: [
        .library(name: "CelluleCore", targets: ["CelluleCore"])
    ],
    targets: [
        .target(name: "CelluleCore"),
        .testTarget(name: "CelluleCoreTests", dependencies: ["CelluleCore"])
    ]
)
