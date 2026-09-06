#!/usr/bin/env node
/*
 * Confronte les fonctions de calcul de la page à des valeurs de référence.
 *
 * Le code testé est extrait de index.html à l'exécution, entre les marqueurs
 * //<pure> et //</pure> : il n'y a donc pas de copie à maintenir en parallèle,
 * et un test ne peut pas passer sur une version du calcul qui n'est plus celle
 * de la page.
 *
 *   node tests/verifier.mjs
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const racine = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const html = readFileSync(path.join(racine, 'index.html'), 'utf8');
const blocs = [...html.matchAll(/\/\/<pure>\r?\n([\s\S]*?)\/\/<\/pure>/g)].map(m => m[1]);
if (blocs.length !== 4) {
  console.error(`Extraction impossible : ${blocs.length} bloc(s) trouvé(s) au lieu de 4.`);
  process.exit(2);
}
const exporte = ['ev2lux','ev2cdm2','lux2ev','luminance','eclairementCiel','luxCrepuscule',
  'masseAir','hauteurSoleil','declinaison','equationTemps','tempsSolaire','midiSolaire',
  'dayOfYear','hauteurFlat','couples','coupleTable','coupleConseille','nearestAp','pick',
  'log2','GRIS','CIEL','OBSTRUCTION','GAINS','MATIERES','ZONES','stopsLabel','situation',
  'fmtLux','fmtAuto','sig3'];
const C = new Function(blocs.join('\n') + `\nreturn {${exporte.join(',')}};`)();

let echecs = 0, total = 0;
const nb = (n, d = 2) => Number.isFinite(n) ? Number(n.toFixed(d)) : n;

function ok(titre, obtenu, attendu, tol) {
  total++;
  const pass = Math.abs(obtenu - attendu) <= tol;
  if (!pass) echecs++;
  console.log(`${pass ? '  ok  ' : 'ÉCHEC '}${titre.padEnd(48)}${String(nb(obtenu)).padStart(10)}   attendu ${attendu} ±${tol}`);
}
function eq(titre, obtenu, attendu) {
  total++;
  const pass = obtenu === attendu;
  if (!pass) echecs++;
  console.log(`${pass ? '  ok  ' : 'ÉCHEC '}${titre.padEnd(48)}${String(obtenu).padStart(10)}   attendu ${attendu}`);
}
const titre = t => console.log(`\n${t}`);

titre('Photométrie — les constantes du système');
ok('EV 15 → lux (C = 250)', C.ev2lux(15), 81920, 1);
ok('EV 15 → cd/m² (K = 12,5)', C.ev2cdm2(15), 4096, 1);
ok('82 000 lx → EV', C.lux2ev(82000), 15.0, 0.02);
ok('L = E·ρ/π sur un gris 18 %', C.luminance(82000, C.GRIS), 4698, 2);
ok('f/16 au 1/125 → EV', C.log2(16 * 16 * 125), 14.97, 0.02);
ok('écart structurel entre C et K', C.lux2ev(82000) - C.log2(C.luminance(82000, C.GRIS) / 0.125), -0.2, 0.03);

titre('Ancrage sur la règle du Sunny 16');
ok('ciel pur, soleil à 52° → EV 15', C.eclairementCiel(52, C.CIEL[0]).ev, 15.0, 0.06);
ok('ciel pur, soleil à 52° → lux', C.eclairementCiel(52, C.CIEL[0]).total, 82000, 3000);
ok('voile léger (doc : −1)', C.eclairementCiel(52, C.CIEL[1]).ev - 15, -1.0, 0.2);
ok('couvert (doc : −2 à −3)', C.eclairementCiel(52, C.CIEL[3]).ev - 15, -2.4, 0.45);
ok('couvert lourd (doc : −4 à −5)', C.eclairementCiel(52, C.CIEL[4]).ev - 15, -4.3, 0.6);

titre('Hauteur du soleil — le sinus contre l’atmosphère');
[[90, 0.4], [60, 0.18], [45, -0.17], [30, -0.77], [15, -1.98], [10, -2.73], [5, -4.0]]
  .forEach(([h, att]) => ok(`soleil à ${h}°, écart au Sunny 16`, C.eclairementCiel(h, C.CIEL[0]).ev - 15, att, 0.12));
ok('sous 15°, le sinus est trop optimiste', (C.eclairementCiel(5, C.CIEL[0]).ev - 15) - C.hauteurFlat(5), -0.5, 0.3);
ok('à 60°, les deux modèles concordent', (C.eclairementCiel(60, C.CIEL[0]).ev - 15) - C.hauteurFlat(60), 0.38, 0.25);

titre('Position du soleil — Corte, 6 septembre 2026');
const n = C.dayOfYear(new Date(2026, 8, 6));
eq('jour de l’année', n, 249);
// Valeurs d'éphéméride pour le 6 septembre 2026 à Corte (42,30° N — 9,15° E, UTC+2).
ok('déclinaison', C.declinaison(n), 6.5, 0.4);
ok('équation du temps (min)', C.equationTemps(n), 1.4, 0.4);
ok('midi solaire, heure légale UTC+2', C.midiSolaire(n, 9.15, 2), 13.37, 0.06);
ok('culmination à 42,3° N', C.hauteurSoleil(42.3, n, 12), 54.2, 0.4);
ok('soleil à 17 h légale (le doc disait 40°)', C.hauteurSoleil(42.3, n, C.tempsSolaire(17, n, 9.15, 2)), 30.2, 0.5);

titre('Solstices et équinoxes');
const ete = C.dayOfYear(new Date(2026, 5, 21));
const hiver = C.dayOfYear(new Date(2026, 11, 21));
const eq_ = C.dayOfYear(new Date(2026, 2, 20));
ok('Corte, solstice d’été (doc : 71°)', C.hauteurSoleil(42.3, ete, 12), 71.1, 0.5);
ok('Corte, équinoxe (doc : 48°)', C.hauteurSoleil(42.3, eq_, 12), 47.7, 0.8);
ok('Corte, solstice d’hiver (doc : 24°)', C.hauteurSoleil(42.3, hiver, 12), 24.3, 0.5);
ok('Gers, solstice d’été (doc : 70°)', C.hauteurSoleil(43.65, ete, 12), 69.8, 0.5);
ok('Gers, solstice d’hiver (doc : 23°)', C.hauteurSoleil(43.65, hiver, 12), 22.9, 0.5);

titre('Table maîtresse — reproduit celle du document');
[[16, 22, '1/125'], [15, 16, '1/125'], [14, 11, '1/125'], [13, 8, '1/125'], [12, 5.6, '1/125'],
 [11, 4, '1/125'], [10, 2.8, '1/125'], [9, 2, '1/125'], [8, 2, '1/60'], [7, 2, '1/30'],
 [6, 2, '1/15'], [5, 2, '1/8'], [3, 2, '1/2'], [0, 2, '4 s'], [-3, 2, '30 s'], [-6, 2, '4 min']]
  .forEach(([ev, N, sh]) => {
    const r = C.coupleTable(ev);
    eq(`EV ${ev}`, r ? `f/${r.near} · ${r.sh.l}` : '—', `f/${N} · ${sh}`);
  });
[[16, '164 000 lx'], [15, '81 900 lx'], [13, '20 500 lx'], [12, '10 200 lx'], [11, '5 120 lx'], [0, '2,5 lx']]
  .forEach(([ev, lx]) => eq(`EV ${ev} → éclairement`, C.fmtLux(C.ev2lux(ev)), lx));

titre('Le cas ciné : 25 fps, 800 EI, plein soleil');
const evCine = 15 + C.log2(8);
ok('EV appareil', evCine, 18, 0.01);
ok('diaph exact au 1/50 (le doc disait f/45)', Math.sqrt(2 ** evCine / 50), 72.4, 0.5);
eq('arrondi au diaph plein', C.nearestAp(Math.sqrt(2 ** evCine / 50)), 64);
ok('sous ND 1,2 — 4 diaphs', Math.sqrt(2 ** (evCine - 4) / 50), 18.1, 0.3);
ok('sous ND 1,8 — 6 diaphs', Math.sqrt(2 ** (evCine - 6) / 50), 9.05, 0.2);

titre('Réflectances et zones');
ok('neige ρ 0,85 → écart au gris', C.log2(0.85 / C.GRIS), 2.24, 0.02);
ok('peau claire ρ 0,35 → +1 diaph', C.log2(0.35 / C.GRIS), 0.96, 0.06);
ok('Zone I en % (le doc disait 0,6)', C.GRIS * 2 ** -4 * 100, 1.125, 0.01);
ok('Zone VII en %', C.GRIS * 2 ** 2 * 100, 72, 0.5);
ok('L* du gris 18 %', 116 * 0.18 ** (1 / 3) - 16, 49.5, 0.1);
eq('la charte grise est bien la zone V', C.ZONES.find(z => z.d === 0).z, 'V');

titre('Cohérence du modèle');
const plein = C.eclairementCiel(52, C.CIEL[0]);
const couvert = C.eclairementCiel(52, C.CIEL[3]);
ok('part de soleil direct, ciel pur', plein.soleil, 1.0, 0.05);
eq('part de soleil direct, couvert', couvert.soleil, 0);
const ombre = C.pick(C.OBSTRUCTION, 'ombre');
const sousbois = C.pick(C.OBSTRUCTION, 'sousbois');
const applique = (o, s) => o.dif + (o.sun - o.dif) * s;
ok('ombre portée au soleil', applique(ombre, plein.soleil), -2.5, 0.05);
eq('ombre portée sous couvert : sans objet', applique(ombre, couvert.soleil), 0);
ok('sous-bois sous couvert : garde son effet', applique(sousbois, couvert.soleil), -3.0, 0.05);

titre('L’exemple de la Restonica, section 12');
const hRestonica = C.hauteurSoleil(42.3, n, C.tempsSolaire(17, n, 9.15, 2));
const ciel = C.eclairementCiel(hRestonica, C.CIEL[0]);
const g = 0.4 + 0.6 * ciel.soleil;
const evScene = ciel.ev
  + applique(C.pick(C.OBSTRUCTION, 'arbre'), ciel.soleil)
  + C.pick(C.GAINS, 'eau').v * g
  + C.pick(C.GAINS, 'sec').v * g;
ok('EV de la scène (le doc annonce 14,2)', evScene, 14.1, 0.25);
// Sur le terrain la vitesse de référence est 1/ISO, pas le 1/125 de la table.
const r = C.coupleConseille(evScene + C.log2(4), 1 / 400);
eq('à 400 ISO, couple de terrain', `f/${r.near} · ${r.sh.l}`, 'f/11 · 1/500');

titre('Mise en forme');
[[0, 'juste'], [-1 / 3, '−⅓'], [2 / 3, '+⅔'], [-4 / 3, '−1 ⅓'], [2, '+2'], [2.24, '+2 ⅓']]
  .forEach(([v, w]) => eq(`stopsLabel(${nb(v)})`, C.stopsLabel(v), w));
[[8192, '8192'], [64, '64'], [8, '8'], [0.5, '0,5'], [0.125, '0,125'], [0.0078125, '0,0078']]
  .forEach(([v, w]) => eq(`fmtAuto(${v})`, C.fmtAuto(v), w));
eq('situation(15)', C.situation(15), 'Plein soleil franc');
eq('situation(8)', C.situation(8), 'Intérieur très éclairé, vitrine');
eq('situation(0)', C.situation(0), 'Crépuscule avancé');

titre('Crépuscule');
ok('h = −6° → lux (crépuscule civil)', C.luxCrepuscule(-6), 3.4, 0.2);
ok('h = −6° → EV (doc : 0,3 à 2)', C.lux2ev(C.luxCrepuscule(-6)), 0.44, 0.2);
ok('h = −12° → lux (crépuscule nautique)', C.luxCrepuscule(-12), 0.008, 0.002);

console.log(`\n${total} vérifications, ${echecs} échec(s).`);
process.exit(echecs ? 1 : 0);
