# Cellule

Une page unique pour exposer à vue, sans posemètre, sur les boîtiers qui n'ont pas
de cellule ou dont on ne sait plus si la leur dit encore la vérité.

Pas de réseau, pas de dépendance, pas de compte. Un seul fichier — `index.html` —
que tu ouvres depuis ton téléphone et qui fonctionne en montagne comme au fond
d'un sous-bois.

## L'installer sur le téléphone

Enregistre `index.html` sur l'appareil et ouvre-le, ou pose le dépôt sur GitHub
Pages et fais « Ajouter à l'écran d'accueil ». Ce que tu règles — lieu, ISO,
thème — et ton journal d'entraînement restent dans le stockage local du
navigateur ; rien ne sort de l'appareil.

## Ce qu'il y a dedans

**Estimer** — la chaîne de facteurs, de la lumière du ciel jusqu'au couple
diaph/vitesse. Tu choisis l'état du ciel, la hauteur du soleil (calculée depuis
le lieu et l'heure, ou estimée d'après ton ombre portée), l'obstacle dominant et
ce qui réverbère autour. Chaque terme s'affiche avec sa valeur, et le total se
lit à la fois en EV et en réglage.

**Convertir** — EV vers couples et retour, l'ombre portée comme goniomètre, et
le placement en zone d'une matière selon sa réflectance.

**Entraîner** — un journal pour noter ce que tu as annoncé et ce que la mesure a
donné. Il ne calcule pas ta justesse mais **ton biais, par type de scène** : c'est
la seule statistique qui serve à quelque chose, parce que l'erreur est presque
toujours systématique et presque toujours dans les ombres. Un mode « à blanc »
tire des scènes au hasard pour les jours où tu ne sors pas.

**Tables** — toutes les tables de référence, calculées et non recopiées.

## Le modèle

L'éclairement direct suit `E₀ · sin(h) · τ^(AM^0,678)`, avec la masse d'air de
Kasten-Young. C'est ce terme qui s'effondre sous 15° — bien plus vite que le
simple `sin(h)` — et qui explique pourquoi la golden hour bascule si vite. S'y
ajoute une composante de ciel diffus en `sin(h)^0,8`, beaucoup plus plate, qui
devient l'essentiel de la lumière quand le soleil est bas ou caché.

Étalonnage : **ciel pur, soleil à 52° → EV 15,0**, c'est-à-dire Sunny 16. C'est
l'ancrage qui reproduit le mieux la règle telle qu'on la pratique, où « plein
soleil » veut dire un soleil haut sans être au zénith.

Deux conséquences que la grille additive classique ne sait pas rendre :

- **par ciel couvert, la hauteur du soleil compte beaucoup moins.** Le terme
  direct est nul, il ne reste que le diffus, qui décroît lentement. Additionner
  « couvert −2,5 » et « soleil bas −1 » compterait deux fois la même perte.
- **une ombre ne coupe que ce qui existe.** Sans soleil direct, « ombre portée »
  ne veut plus rien dire, alors que « sous-bois » garde tout son sens puisque la
  canopée bouche le ciel lui-même. Les facteurs d'obstruction sont donc pondérés
  par la part de soleil direct réellement présente.

Un sélecteur **grille mentale** désactive tout ça et rend les facteurs plats du
document de référence — ce que tu calcules de tête. L'écart entre les deux
chiffres te dit exactement où l'approximation lâche.

## Le document source

`docs/echelle-lumiere.md` est le document de référence, relu et corrigé. Six
erreurs y ont été trouvées et sont signalées sur place, avec un récapitulatif en
fin de fichier :

| § | Le document disait | Valeur exacte |
|---|---|---|
| 9 | Zone I à 0,6 % de réflectance | 1,1 % |
| 11 | 25 fps, 800 EI, plein soleil → f/45 | f/64 |
| 11 | Avec ND1,2 → f/11 ; ND1,8 → f/5,6 | f/18 et f/9 |
| 5 | Midi solaire à Corte : 13 h 10 | 13 h 22 |
| 12 | Restonica : soleil à ~40° à 17 h | 30,2° |
| 6 | Journée couverte : 1 000–10 000 lx | 1 000–20 000 lx |

Deux réserves ont été ajoutées plutôt que corrigées : l'écart structurel de
0,2 diaph entre C = 250 et K = 12,5, et le fait que la table en `log₂(sin h)`
devient optimiste dès 10° de hauteur, pas seulement sous 5°.

## Vérification

`tests/verifier.mjs` extrait les fonctions de calcul de `index.html` et les
confronte à une cinquantaine de valeurs de référence : photométrie de base,
ancrage Sunny 16, hauteurs solaires aux solstices pour Corte et le Gers, cas
ciné, réflectances, zones, et l'exemple de la Restonica.

```
node tests/verifier.mjs
```

Aucune dépendance à installer.

## Ce que les chiffres ne disent pas

Le modèle suppose un ciel clair et propre. Brume de chaleur, poussière, fumée
d'incendie, pollution : chacune retire de ½ à 2 diaphs sans prévenir, et l'œil
s'y adapte trop bien pour les voir. Les réflectances sont des médianes — de
l'asphalte mouillé chute vers 0,05, de l'herbe sèche perd 0,10 sur de l'herbe
irriguée.

Et en cas de doute avec du négatif : surexpose. L'asymétrie est réelle et large.
