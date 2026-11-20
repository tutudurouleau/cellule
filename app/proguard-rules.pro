# Le code de l'appli reste tel quel (noms des enums et des champs relus
# dans les données enregistrées, JSON écrit à la main) : R8 n'optimise et
# n'allège que les bibliothèques (Compose, CameraX, ZXing).
-keep class fr.cellule.** { *; }
-keepattributes SourceFile,LineNumberTable
