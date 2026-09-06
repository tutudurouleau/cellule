# Cellule

Exposer à vue, sans posemètre — et mesurer quand même, avec ce qu'on a dans la
poche.

Deux outils dans ce dépôt, qui partagent la même photométrie :

- **`index.html`** — une page unique, sans réseau ni dépendance, à enregistrer
  sur le téléphone. Elle estime, convertit, entraîne et récapitule.
- **`app/`** — une application Android qui fait tout cela **et mesure**, en se
  servant de la caméra et du capteur de luminosité ambiante.

---

## L'application Android

### Récupérer l'APK

**[cellule.apk](https://github.com/tutudurouleau/cellule/releases/download/apk/cellule.apk)**

Ouvre ce lien depuis le téléphone. Android télécharge le fichier, puis demande
d'autoriser l'installation depuis cette source : c'est normal pour un APK non
signé par le Play Store. Aucune donnée ne sort de l'appareil — pas de réseau,
pas de compte, pas de télémétrie.

Le lien ne change jamais : le workflow `.github/workflows/compiler.yml` compile
à chaque envoi et remplace le fichier de la Release `apk`. Le même APK se
trouve aussi en artefact d'Actions, mais en zip et derrière une connexion — le
lien direct est plus commode.

Pour compiler soi-même, avec le SDK Android installé :

```
./gradlew :app:assembleDebug
```

### Les deux capteurs, et pourquoi ils ne disent pas la même chose

| | Caméra | Capteur d'ambiance |
|---|---|---|
| Ce qu'elle mesure | la lumière qui **repart** du sujet | la lumière qui **arrive** |
| Équivalent argentique | spotmètre, cellule réfléchie | cellule à dôme, incidente |
| Dépend de la matière visée | **oui** — suppose ρ = 0,18 | non |
| Plage utile | très large | sature avant le plein soleil |
| Finesse | au tiers de diaph | ordre de grandeur |

**La caméra** : quand l'exposition automatique a convergé, le triplet qu'elle a
choisi — ouverture, temps de pose, sensibilité — encode exactement la luminance
de ce qu'elle vise, puisque son travail était précisément de la placer sur le
gris moyen. On lit ces métadonnées dans `CaptureResult` et on remonte à
l'EV. C'est un spotmètre, avec le piège du spotmètre : sur la neige il lira
2⅓ diaphs de trop, et suivre sa lecture rendra la neige grise.

**Le capteur d'ambiance** donne des lux directement. C'est la mesure qu'on
veut — celle qui ignore la couleur du sujet — mais l'organe est grossier : il
bute vers 30 000 lux sur beaucoup d'appareils, quantifie mal dans le bas, et se
trouve derrière la vitre de l'écran, dont la réponse angulaire n'a rien de
cosinusoïdale. L'application signale la saturation dès qu'elle survient plutôt
que d'afficher un chiffre faux.

### Les trois modes

**Spot** — un disque déplaçable dans l'aperçu, redimensionnable. C'est le mode
du zone system : vise l'ombre la plus sombre où tu veux encore de la matière,
place-la en zone III, et l'exposition retenue s'en déduit. Les lectures
suivantes affichent alors leur propre zone. Mémorise-en trois ou quatre et
l'application donne l'amplitude de la scène, à comparer à la plage du support.

**Moyenne** — tout le cadre, tel que l'appareil l'a décidé. Bon sur une scène
ordinaire, piégeux dès qu'un ciel occupe le tiers de l'image.

**Incident** — le capteur d'ambiance, écran tourné vers la source.

### Deux précautions qui décident de la justesse

**Les deux flux ne sont pas la même image.** Les métadonnées viennent du flux
d'aperçu, la luminance du flux d'analyse. Tant que l'exposition automatique
cherche encore, ils ne se correspondent pas et la lecture ne vaut rien.
L'application lit `CONTROL_AE_STATE` et le dit à l'écran ; le bouton **Figer**
verrouille l'exposition côté matériel, ce qui aligne définitivement les deux.

**Le plan de luminance est encodé en gamma.** Il faut linéariser chaque pixel
*avant* de moyenner. Moyenner les valeurs gamma puis linéariser sous-estime les
hautes lumières de plus d'un diaph sur une scène contrastée — il y a un test
dédié à cette différence.

### Étalonner

Deux inconnues que l'API Android ne renseigne pas : la valeur exacte à laquelle
l'algorithme d'exposition place le gris moyen, qui varie d'un téléphone à
l'autre, et la plage du plan Y — vidéo 16-235 ou pleine 0-255 selon le
constructeur.

L'étalonnage les absorbe d'un coup. Vise une surface dont tu connais la nature,
indique ce qu'annonce une cellule en qui tu as confiance, et le décalage est
mémorisé puis retiré de toutes les lectures. Une charte grise est l'idéal ;
n'importe quelle matière de la table fonctionne.

Si la charte lit systématiquement à côté d'un demi-diaph environ, essaie
l'autre plage de luminance avant de rattraper à l'étalonnage : c'est
probablement là qu'est le problème.

---

## Le modèle d'estimation

L'éclairement direct suit `E₀ · sin(h) · τ^(AM^0,678)`, avec la masse d'air de
Kasten-Young. C'est ce terme qui s'effondre sous 15° — bien plus vite que le
simple `sin(h)` — et qui explique pourquoi la golden hour bascule si vite. S'y
ajoute une composante de ciel diffus en `sin(h)^0,8`, beaucoup plus plate, qui
devient l'essentiel de la lumière quand le soleil est bas ou caché.

Étalonnage : **ciel pur, soleil à 52° → EV 15,0**, c'est-à-dire Sunny 16.

Deux conséquences que la grille additive classique ne sait pas rendre :

- **par ciel couvert, la hauteur du soleil compte beaucoup moins.** Le terme
  direct est nul, il ne reste que le diffus, qui décroît lentement. Additionner
  « couvert −2,5 » et « soleil bas −1 » compterait deux fois la même perte.
- **une ombre ne coupe que ce qui existe.** Sans soleil direct, « ombre portée »
  ne veut plus rien dire, alors que « sous-bois » garde tout son sens puisque la
  canopée bouche le ciel lui-même.

Un sélecteur **grille mentale** désactive tout cela et rend les facteurs plats
du document de référence — ce que tu calcules de tête. L'écart entre les deux
chiffres te dit exactement où l'approximation lâche.

La position du soleil emploie les séries de Spencer : la formule courte en
`cos(0,98563·(n−173))` dérive de plus d'un degré près des équinoxes.

---

## Le journal d'entraînement

Il ne mesure pas ta justesse mais **ton biais, par type de scène**. C'est la
seule statistique qui serve : l'erreur est presque toujours systématique, et
presque toujours dans les ombres. Sous quatre entrées pour un type donné, il
refuse de conclure.

Dans l'application, l'écran de mesure verse directement l'EV mesuré dans le
journal, et l'écran d'estimation y verse la chaîne annoncée. Annoncer, mesurer,
comparer devient l'affaire de trois touches.

Le protocole tient en trois phases : **ancrage** (annonce et note l'écart, deux
à trois semaines), **décomposition** (annonce la chaîne, pas le chiffre, un à
deux mois), **reconnaissance** (le chiffre arrive sans calcul).

---

## Structure

```
index.html              la page hors-ligne, autonome
tests/verifier.mjs      ses 89 vérifications
core/                   Kotlin pur — toute la photométrie, zéro Android
app/                    l'application : mesure, estimation, journal, tables
docs/echelle-lumiere.md le document de référence, relu et corrigé
```

`core` ne dépend d'aucune bibliothèque Android, ce qui n'est pas un détail de
rangement : la photométrie s'y teste sans appareil, sans émulateur et sans le
SDK. Chaque module déclare ses propres plugins Gradle, si bien que
`./gradlew :core:test` ne contacte jamais le dépôt Google.

```
./gradlew :core:test    57 vérifications  (photométrie, posemètre, soleil)
node tests/verifier.mjs 89 vérifications  (la page hors-ligne)
```

Ce que les tests couvrent, entre autres : l'étalonnage Sunny 16, la reproduction
exacte de la table maîtresse du document, les hauteurs solaires aux solstices
pour Corte et le Gers, le décodage du gris moyen à 18 % dans les deux plages, la
différence entre moyenner en linéaire et moyenner en gamma, le fait qu'une ombre
portée s'annule sous un ciel couvert alors qu'un sous-bois non, et l'aller-retour
du repère de visée entre l'écran et le capteur.

---

## Le document source

`docs/echelle-lumiere.md` est le document de référence, relu et corrigé. Six
erreurs y ont été trouvées et sont signalées sur place :

| § | Le document disait | Valeur exacte |
|---|---|---|
| 9 | Zone I à 0,6 % de réflectance | 1,1 % |
| 11 | 25 fps, 800 EI, plein soleil → f/45 | f/64 |
| 11 | Avec ND1,2 → f/11 ; ND1,8 → f/5,6 | f/18 et f/9 |
| 5 | Midi solaire à Corte : 13 h 10 | 13 h 22 |
| 12 | Restonica : soleil à ~40° à 17 h | 30,2° |
| 6 | Journée couverte : 1 000–10 000 lx | 1 000–20 000 lx |

Deux réserves ont été ajoutées plutôt que corrigées : l'écart structurel de
0,2 diaph entre C = 250 et K = 12,5 (la cohérence exigerait C ≈ 218), et le fait
que la table en `log₂(sin h)` devient optimiste dès 10° de hauteur, pas
seulement sous 5°.

---

## Ce que ces chiffres ne disent pas

Le modèle atmosphérique suppose un ciel clair et propre. Brume de chaleur,
poussière, fumée d'incendie, pollution : chacune retire de ½ à 2 diaphs sans
prévenir, et l'œil s'y adapte trop bien pour les voir.

Les réflectances sont des médianes. De l'asphalte mouillé chute vers 0,05 ; de
l'herbe sèche perd 0,10 sur de l'herbe irriguée.

Et en cas de doute avec du négatif : surexpose. L'asymétrie est réelle et large.
