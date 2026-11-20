# Mémoire du projet Cellule (à lire en premier)

Appli Android de photo et de cinéma pour un étudiant en cinéma (prépa Louis-Lumière),
qui teste sur son téléphone.

- **Où on en est** : la **v5.0** est codée (les 5 étapes de `docs/v5/PLAN-V5.md`).
  Reste pour plus tard : l'éditeur de plans sur PC (**v5.5**, même format de plan,
  transfert par QR/texte déjà en place : `core/PlanCodec.kt`, `sol/TransfertPlan.kt`).
- Web : la session n'atteint pas les sites ; les machines de GitHub oui. Relevés des
  loueurs : `outils/loueurs.json` → workflow « Loueurs » → `outils/releves/`, puis
  `python3 -I outils/dispo.py` régénère `core/DisponibiliteLoueurs.kt`. Photos libres :
  `outils/photos.json` → workflow « Photos » (candidates dans `outils/candidats/`, on
  choisit avec « choix »).
- Guide de l'interface et carte des fichiers : `docs/refonte-interface/LISEZ-MOI.md`.
- Branche de travail : `claude/adoring-lamport-yln7ao`. Pas de PR sans demande.

## Façon de travailler

- Réponds en français, en tutoyant, court et concret.
- Une étape = un commit = un APK envoyé = un retour de l'utilisateur.
- `core` (Kotlin pur) : `./gradlew :core:test -q` en local. `app` ne compile que sur
  GitHub Actions (workflow « Compiler »), qui publie l'APK sur la release `apk`.
  Si le job « Publier l'APK » échoue par concurrence, relance les jobs échoués.
- Les commits qui ne touchent que la doc prennent `[skip ci]`.
- Ne touche pas à `docs/coupes-objectifs/`.
- Piège K2 : deux déclarations `private` de même nom dans deux fichiers du même
  paquet entrent en conflit. Vérifie les noms (grep) avant de pousser.
- Chiffres techniques : publiés par le fabricant, sources citées. Sinon on affiche
  « à vérifier », jamais un chiffre de seconde main.
- Navigation : lanceur « Tout », sommaires en cartes, onglets soulignés ;
  `ChoixSegmente` ne sert qu'à choisir une valeur.
