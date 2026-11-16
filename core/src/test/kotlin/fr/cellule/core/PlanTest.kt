package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanTest {

    /** Un téléphone qui voit comme un 26 mm en 24×36, photo 4:3 couchée. */
    private val telephone = Cadreur.champDepuisEquivalent(26.0, 4.0 / 3.0)

    @Test
    fun `le champ d un equivalent 24x36 retrouve la diagonale`() {
        val champ = Cadreur.champDepuisEquivalent(43.27, 3.0 / 2.0)
        /* À 43,27 mm sur du 3:2, on retrouve les angles du 24×36 lui-même. */
        assertEquals(Focales.angleDeChamp(36.0, 43.27), champ.horizontal, 0.05)
        assertEquals(Focales.angleDeChamp(24.0, 43.27), champ.vertical, 0.05)
    }

    @Test
    fun `une focale plus longue que le telephone s inscrit dans la photo`() {
        /* Capteur 24 × 18 mm (4:3) à 50 mm, dans un téléphone équivalent 26 mm. */
        val cadre = CadrePlan("essai", 24.0, 18.0, 50.0)
        val trace = assertNotNull(Cadreur.tracer(telephone, cadre))
        assertFalse(trace.tropLarge)
        assertTrue(trace.capteur.largeur in 0.2..0.9)
        /* Même proportions des deux côtés : le cadre occupe la même part en largeur et en hauteur. */
        assertEquals(trace.capteur.largeur, trace.capteur.hauteur, 0.01)
        /* Centré. */
        assertEquals(1.0, trace.capteur.gauche * 2 + trace.capteur.largeur, 1e-9)
        assertEquals(1.0, trace.capteur.haut * 2 + trace.capteur.hauteur, 1e-9)
        assertEquals(Focales.angleDeChamp(24.0, 50.0), trace.champHorizontal, 1e-9)
    }

    @Test
    fun `doubler la focale divise le cadre par deux`() {
        val a = assertNotNull(Cadreur.tracer(telephone, CadrePlan("", 24.0, 18.0, 50.0)))
        val b = assertNotNull(Cadreur.tracer(telephone, CadrePlan("", 24.0, 18.0, 100.0)))
        assertEquals(a.capteur.largeur / 2, b.capteur.largeur, 1e-9)
        assertEquals(a.capteur.hauteur / 2, b.capteur.hauteur, 1e-9)
    }

    @Test
    fun `une focale trop large pour le telephone est annoncee, pas dessinee`() {
        val trace = assertNotNull(Cadreur.tracer(telephone, CadrePlan("", 36.0, 24.0, 14.0, formats = listOf("2.39:1"))))
        assertTrue(trace.tropLarge)
        assertTrue(trace.formats.isEmpty())
        /* Sur du 24×36, ce téléphone ne montre rien de plus large que son propre équivalent, à peu près. */
        assertTrue(trace.focaleMinimale in 24.0..30.0)
        /* À la focale minimale, tout rentre juste. */
        val juste = assertNotNull(Cadreur.tracer(telephone, CadrePlan("", 36.0, 24.0, trace.focaleMinimale)))
        assertFalse(juste.tropLarge)
    }

    @Test
    fun `l anamorphose elargit le champ horizontal seulement`() {
        val spherique = assertNotNull(Cadreur.tracer(telephone, CadrePlan("", 24.0, 18.0, 100.0)))
        val anamorphique = assertNotNull(Cadreur.tracer(telephone, CadrePlan("", 24.0, 18.0, 100.0, desanamorphose = 2.0)))
        assertEquals(spherique.capteur.largeur * 2, anamorphique.capteur.largeur, 1e-9)
        assertEquals(spherique.capteur.hauteur, anamorphique.capteur.hauteur, 1e-9)
        assertEquals(listOf(1.0, 1.3, 1.5, 1.8, 2.0), DESANAMORPHOSES)
    }

    @Test
    fun `les formats se tracent a l interieur du capteur`() {
        val cadre = CadrePlan("", 28.0, 18.0, 75.0, formats = listOf("2.39:1", "1.85:1", "4:5", "inconnu"))
        val trace = assertNotNull(Cadreur.tracer(telephone, cadre))
        assertEquals(listOf("2.39:1", "1.85:1", "4:5"), trace.formats.map { it.nom })
        trace.formats.forEach { f ->
            assertTrue(f.rect.largeur <= trace.capteur.largeur + 1e-9)
            assertTrue(f.rect.hauteur <= trace.capteur.hauteur + 1e-9)
            assertTrue(f.rect.gauche >= trace.capteur.gauche - 1e-9)
            assertTrue(f.rect.bas <= trace.capteur.bas + 1e-9)
        }
        /* Le Scope prend toute la largeur ; le 4:5 toute la hauteur. */
        assertEquals(trace.capteur.largeur, trace.formats[0].rect.largeur, 1e-9)
        assertEquals(trace.capteur.hauteur, trace.formats[2].rect.hauteur, 1e-9)
    }

    @Test
    fun `sans capteur ni focale, rien a tracer`() {
        assertNull(Cadreur.tracer(telephone, CadrePlan()))
        assertNull(Cadreur.tracer(telephone, CadrePlan("", 24.0, 18.0, null)))
    }

    @Test
    fun `les formats du cahier des charges existent tous`() {
        val attendus = listOf(1.33, 1.37, 1.43, 1.66, 1.78, 1.85, 1.90, 2.00, 2.20, 2.39, 2.76)
        attendus.forEach { r ->
            assertTrue(RATIOS_CINE.any { kotlin.math.abs(it.ratio - r) < 0.006 }, "format $r manquant")
        }
        assertEquals(0.8, TOUS_LES_RATIOS.first { it.nom == "4:5" }.ratio, 1e-9)
        assertEquals(0.5625, TOUS_LES_RATIOS.first { it.nom == "9:16" }.ratio, 1e-9)
        /* Un nom ne désigne qu'un format : c'est par lui qu'un plan s'en souvient. */
        assertEquals(TOUS_LES_RATIOS.size, TOUS_LES_RATIOS.map { it.nom }.toSet().size)
    }

    @Test
    fun `le soleil est au sud a midi solaire et se leve a l est`() {
        val jour = 172 /* solstice d'été */
        assertEquals(180.0, Soleil.azimut(48.86, jour, 12.0), 0.01)
        val matin = Soleil.azimut(48.86, jour, 7.0)
        val soir = Soleil.azimut(48.86, jour, 17.0)
        assertTrue(matin in 60.0..110.0, "matin $matin")
        assertTrue(soir in 250.0..300.0, "soir $soir")
        /* Symétrie autour du midi solaire. */
        assertEquals(360.0, matin + soir, 0.01)
        /* Aux équinoxes, le soleil se lève plein est (hauteur nulle à 6 h solaires). */
        assertEquals(90.0, Soleil.azimut(45.0, 80, 6.0), 1.5)
    }

    @Test
    fun `la boussole parle en points cardinaux`() {
        assertEquals("N", Boussole.cardinal(0.0))
        assertEquals("N", Boussole.cardinal(350.0))
        assertEquals("NE", Boussole.cardinal(45.0))
        assertEquals("S", Boussole.cardinal(181.0))
        assertEquals("O", Boussole.cardinal(-90.0))
        assertEquals("225° SO", Boussole.libelle(225.0))
    }

    @Test
    fun `le lieu donne ses coordonnees et un lien Maps`() {
        val lieu = LieuPlan(43.6045, 1.4440, 8.0, 146.0)
        assertEquals("https://www.google.com/maps?q=43.604500,1.444000", lieu.lienMaps)
        assertEquals("43,60450° N  1,44400° E", lieu.coordonnees)
        assertEquals("33,00000° S  70,50000° O", LieuPlan(-33.0, -70.5).coordonnees)
        assertEquals(0.0, distanceMetres(43.0, 1.0, 43.0, 1.0), 1e-6)
        /* Un degré de latitude : 111 km. */
        assertEquals(111_195.0, distanceMetres(43.0, 1.0, 44.0, 1.0), 50.0)
    }

    @Test
    fun `la reference d un plan suit la numerotation choisie`() {
        val p = Plan(1, "2026-11-16", sequence = "3", plan = "2", prise = "1", titre = "La cuisine")
        assertEquals("Séq. 3 · Plan 2 · Prise 1", p.reference)
        assertEquals("Séq. 3 · Plan 2 · Prise 1 — La cuisine", p.intitule)
        assertEquals("Plan 12", Plan(2, "", numerotation = Numerotation.SIMPLE, numero = "12").reference)
        assertEquals("Plan sans numéro", Plan(3, "").reference)
    }

    @Test
    fun `le plan suivant reprend la sequence et avance la prise`() {
        val premier = CarnetCinema.suivant(null, Numerotation.SEQUENCE)
        assertEquals(listOf("1", "1", "1"), listOf(premier.sequence, premier.plan, premier.prise))
        val p = Plan(1, "", sequence = "3", plan = "2B", prise = "4")
        val s = CarnetCinema.suivant(p, Numerotation.SEQUENCE)
        assertEquals(listOf("3", "2B", "5"), listOf(s.sequence, s.plan, s.prise))
        assertEquals("1", CarnetCinema.suivant(null, Numerotation.SIMPLE).numero)
        assertEquals("13", CarnetCinema.suivant(Plan(1, "", numerotation = Numerotation.SIMPLE, numero = "12"), Numerotation.SIMPLE).numero)
    }

    private fun carnet(): CarnetCinemaDonnees = CarnetCinemaDonnees(
        projets = listOf(Projet(1, "Le film"), Projet(2, "Le clip")),
        jours = listOf(JourCinema(10, 1, "Jour 1", "2026-11-16"), JourCinema(11, 1, "Repérage", "2026-11-10", TypeJour.REPERAGE), JourCinema(20, 2, "Jour 1")),
        plans = listOf(Plan(100, "2026-11-16", jourId = 10), Plan(101, "2026-11-16", jourId = 10), Plan(102, "2026-11-10", jourId = 11), Plan(103, "")),
        liens = listOf(LienVue(500, 1))
    )

    @Test
    fun `la hierarchie projet - jour - plans se parcourt`() {
        val c = carnet()
        /* Les jours d'un projet, dans l'ordre des dates. */
        assertEquals(listOf(11L, 10L), c.joursDe(1).map { it.identifiant })
        assertEquals(listOf(100L, 101L), c.plansDe(10).map { it.identifiant })
        assertEquals(listOf(102L, 100L, 101L), c.plansDuProjet(1).map { it.identifiant })
        assertEquals(listOf(103L), c.plansNonRanges.map { it.identifiant })
        assertEquals(listOf(500L), c.vuesDe(1))
        assertEquals(1L, c.projetDeLaVue(500))
        assertNull(c.projetDeLaVue(501))
    }

    @Test
    fun `supprimer un jour ou un projet ne perd aucun plan`() {
        val sansJour = CarnetCinema.sansJour(carnet(), 10)
        assertEquals(4, sansJour.plans.size)
        assertEquals(listOf(100L, 101L, 103L), sansJour.plansNonRanges.map { it.identifiant })
        val sansProjet = CarnetCinema.sansProjet(carnet(), 1)
        assertEquals(listOf(2L), sansProjet.projets.map { it.identifiant })
        assertEquals(listOf(20L), sansProjet.jours.map { it.identifiant })
        assertEquals(4, sansProjet.plans.size)
        assertEquals(4, sansProjet.plansNonRanges.size)
        assertTrue(sansProjet.liens.isEmpty())
    }

    @Test
    fun `deplacer, modifier et lier`() {
        val d = CarnetCinema.deplacer(carnet(), 103, 20)
        assertEquals(listOf(103L), d.plansDe(20).map { it.identifiant })
        val m = CarnetCinema.remplacer(d, d.plans.first { it.identifiant == 100L }.copy(titre = "Modifié"))
        assertEquals("Modifié", m.plans.first { it.identifiant == 100L }.titre)
        assertEquals(4, m.plans.size)
        assertEquals(5, CarnetCinema.remplacer(m, Plan(999, "")).plans.size)
        /* Une vue n'est rangée que dans un projet à la fois. */
        val l = CarnetCinema.lier(carnet(), 500, 2)
        assertEquals(listOf(LienVue(500, 2)), l.liens)
        assertTrue(CarnetCinema.lier(l, 500, null).liens.isEmpty())
    }

    @Test
    fun `restaurer deux fois ne dedouble rien`() {
        val ancien = carnet()
        val sauvegarde = ancien.copy(
            projets = ancien.projets + Projet(3, "Nouveau"),
            plans = ancien.plans.map { it.copy(titre = "version de la sauvegarde") } + Plan(104, ""),
            liens = ancien.liens + LienVue(501, 2)
        )
        val une = CarnetCinema.fusionner(ancien, sauvegarde)
        assertEquals(3, une.projets.size)
        assertEquals(5, une.plans.size)
        assertEquals(2, une.liens.size)
        /* Ce qu'on avait n'est pas écrasé. */
        assertEquals("", une.plans.first { it.identifiant == 100L }.titre)
        assertEquals(une, CarnetCinema.fusionner(une, sauvegarde))
    }

    @Test
    fun `la page de garde d un dossier`() {
        val plans = listOf(
            Plan(1, "2026-11-16", lieu = LieuPlan(43.6, 1.44, adresse = "Place du Capitole, Toulouse")),
            Plan(2, "2026-11-14", lieu = LieuPlan(43.6, 1.44, adresse = "Place du Capitole, Toulouse")),
            Plan(3, "2026-11-15", lieu = LieuPlan(43.65, 0.59))
        )
        assertEquals(listOf("Place du Capitole, Toulouse", "43,65000° N  0,59000° E"), CarnetCinema.lieux(plans))
        assertEquals("2026-11-14 → 2026-11-16", CarnetCinema.periode(plans, emptyList()))
        assertEquals("2026-11-16", CarnetCinema.periode(plans.take(1), emptyList()))
        assertEquals("", CarnetCinema.periode(emptyList(), emptyList()))
    }

    @Test
    fun `le resume de l exposition et du cadre`() {
        val e = ExpositionPlan(12.0, true, 24.0, 172.8, 800, "T4", "1/50 s", "ND 0,9")
        assertEquals("T4 · 1/50 s · EI 800 · 24 im/s · 172,8°", e.resume)
        val c = CadrePlan("ALEXA Mini — Open Gate", 28.25, 18.17, 40.0, 2.0, listOf("2.39:1"))
        assertEquals("ALEXA Mini — Open Gate · 28,25 × 18,17 mm · 40 mm · anamorphique ×2 · 2.39:1", c.resume)
        assertTrue(c.complet)
    }
}
