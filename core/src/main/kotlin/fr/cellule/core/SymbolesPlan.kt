package fr.cellule.core

/*
 * Les symboles du plan de feu. Il n'existe pas de norme officielle unique au
 * cinéma : on suit les conventions que partagent les plans de feu du théâtre
 * (symboles de l'USITT) et les logiciels de plans de tournage — une forme par
 * optique (Fresnel, ouvert, découpe, PAR, panneau…), vue de dessus, l'avant
 * vers le sujet, et une lettre pour la source (T, H, L, F).
 */

enum class SymboleProjecteur(val libelle: String, val detail: String) {
    FRESNEL("Fresnel", "lentille à échelons : faisceau doux, réglable spot-flood"),
    OUVERT("Open face", "réflecteur sans lentille : blonde, mandarine, Redhead"),
    DECOUPE("Découpe", "faisceau net, découpé par des couteaux ou un gobo"),
    PAR("PAR / réflecteur", "faisceau intense : PAR, HMI à réflecteur, LED COB nue"),
    PANNEAU("Panneau LED", "grande surface : SkyPanel, Litemat, Gemini"),
    TUBE("Tube", "tube LED : Astera, Titan, Quasar"),
    BANQUE("Banque fluo", "tubes en batterie : Kino Flo"),
    BALLON("Ballon / spacelight", "lumière d'ambiance dans toutes les directions"),
    BOITE("Boîte à lumière", "projecteur sous softbox, Chimera, lanterne"),
    AUTRE("Autre", "symbole générique")
}

enum class SourceLampe(val lettre: String, val libelle: String) {
    TUNGSTENE("T", "tungstène (3200 K)"),
    HMI("H", "HMI, lumière du jour"),
    LED("L", "LED"),
    FLUO("F", "fluorescent")
}

object SymbolesPlan {

    private fun contient(nom: String, vararg mots: String): Boolean {
        val n = sansAccents(nom).lowercase()
        return mots.any { Regex("(^|[^a-z0-9])" + Regex.escape(it) + "($|[^a-z0-9])").containsMatchIn(n) }
    }

    /** La forme d'un projecteur du catalogue : par famille, affinée par son nom. */
    fun symbole(p: Projecteur): SymboleProjecteur {
        val n = p.nomComplet
        return when {
            contient(n, "chimera", "softbox", "lantern", "lanterne", "dome", "octa") -> SymboleProjecteur.BOITE
            contient(n, "fresnel", "f10", "f14", "fresnel lens") -> SymboleProjecteur.FRESNEL
            else -> when (p.famille) {
                FamilleProjecteur.PANNEAU_LED -> SymboleProjecteur.PANNEAU
                FamilleProjecteur.LED_PONCTUELLE -> SymboleProjecteur.PAR
                FamilleProjecteur.TUBE_LED -> SymboleProjecteur.TUBE
                FamilleProjecteur.FRESNEL_TUNGSTENE, FamilleProjecteur.FRESNEL_LED -> SymboleProjecteur.FRESNEL
                FamilleProjecteur.OUVERT_TUNGSTENE -> SymboleProjecteur.OUVERT
                FamilleProjecteur.HMI ->
                    if (contient(n, "par", "m18", "m40", "m90", "joker", "pocket par", "lightning strikes")) SymboleProjecteur.PAR
                    else SymboleProjecteur.FRESNEL
                FamilleProjecteur.FLUO -> SymboleProjecteur.BANQUE
                FamilleProjecteur.DECOUPE -> SymboleProjecteur.DECOUPE
                FamilleProjecteur.AMBIANCE -> SymboleProjecteur.BALLON
                FamilleProjecteur.AUTRE -> SymboleProjecteur.AUTRE
            }
        }
    }

    fun source(p: Projecteur): SourceLampe? = when (p.famille) {
        FamilleProjecteur.PANNEAU_LED, FamilleProjecteur.LED_PONCTUELLE, FamilleProjecteur.TUBE_LED,
        FamilleProjecteur.FRESNEL_LED -> SourceLampe.LED
        FamilleProjecteur.FRESNEL_TUNGSTENE, FamilleProjecteur.OUVERT_TUNGSTENE -> SourceLampe.TUNGSTENE
        FamilleProjecteur.HMI -> SourceLampe.HMI
        FamilleProjecteur.FLUO -> SourceLampe.FLUO
        FamilleProjecteur.DECOUPE, FamilleProjecteur.AMBIANCE, FamilleProjecteur.AUTRE ->
            when {
                contient(p.nomComplet, "led") -> SourceLampe.LED
                contient(p.nomComplet, "hmi") -> SourceLampe.HMI
                p.famille == FamilleProjecteur.DECOUPE -> SourceLampe.TUNGSTENE
                else -> null
            }
    }

    /** Le symbole d'un élément du plan : celui choisi à la main, sinon celui de son modèle. */
    fun symbole(e: ElementSol, modele: Projecteur?): SymboleProjecteur =
        SymboleProjecteur.entries.firstOrNull { it.name == e.symbole } ?: modele?.let { symbole(it) } ?: SymboleProjecteur.AUTRE

    fun source(e: ElementSol, modele: Projecteur?): SourceLampe? =
        SourceLampe.entries.firstOrNull { it.name == e.lampe } ?: modele?.let { source(it) }

    /** « 1,2 kW », « 450 W » : la puissance comme on la dit sur un plan. */
    fun puissance(w: Double): String = if (w >= 1000) nombreCourt(w / 1000, 1) + " kW" else nombreCourt(w, 0) + " W"

    /** La seconde ligne de l'étiquette : « 1,2 kW · ½ CTO · circ. A ». */
    fun etiquette(e: ElementSol, modele: Projecteur?): String = listOfNotNull(
        (modele?.puissanceW ?: e.puissanceW)?.let { puissance(it) },
        e.gelatine.takeIf { it.isNotBlank() },
        e.circuit.takeIf { it.isNotBlank() }?.let { "circ. $it" }
    ).joinToString(" · ")

    /** Les gélatines qu'on écrit le plus souvent sur un plan. */
    val GELATINES = listOf(
        "Full CTO", "1/2 CTO", "1/4 CTO", "1/8 CTO", "Full CTB", "1/2 CTB", "1/4 CTB",
        "Plus green", "Minus green", "216", "250", "251", "129", "Opal", "Grid cloth"
    )
}
