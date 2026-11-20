package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ListeLocationTest {

    private fun projo(id: Long, modele: String, circuit: String = "") =
        ElementSol(id, TypeElement.PROJECTEUR, 0.0, 0.0, projecteur = modele, circuit = circuit)

    private val nom: (ElementSol) -> String = { it.projecteur.ifBlank { "Sans modèle" } }

    @Test
    fun `la liste d'un plan`() {
        val plan = PlanSol(
            1, "Salon", elements = listOf(
                ElementSol(1, TypeElement.CAMERA, 0.0, 0.0, nom = "A"),
                ElementSol(2, TypeElement.CAMERA, 1.0, 0.0, nom = "A", etape = 2),
                projo(3, "SkyPanel"), projo(4, "SkyPanel"), projo(5, "Fresnel 650"),
                ElementSol(6, TypeElement.DIFFUSION, 0.0, 0.0, largeur = 1.2),
                ElementSol(7, TypeElement.RAIL, 0.0, 0.0, largeur = 3.0),
                ElementSol(8, TypeElement.RAIL, 0.0, 0.0, largeur = 2.0),
                ElementSol(9, TypeElement.MUR, 0.0, 0.0)
            )
        )
        val l = ListeLocation.duPlan(plan, nom).associate { it.nom to it.combien }
        assertEquals("× 1", l["Caméras"])
        assertEquals("× 2", l["SkyPanel"])
        assertEquals("× 1", l["Fresnel 650"])
        assertEquals("× 1", l["Cadre de diffusion de 1,2 m"])
        assertEquals("5 m", l["Rail de travelling"])
        assertEquals(5, l.size)
    }

    @Test
    fun `un jour prend le maximum de ses plans`() {
        val a = PlanSol(1, "a", elements = listOf(projo(1, "SkyPanel"), projo(2, "SkyPanel")))
        val b = PlanSol(2, "b", elements = listOf(projo(3, "SkyPanel"), projo(4, "Fresnel 650")))
        val l = ListeLocation.duJour(listOf(a, b), nom).associate { it.nom to it.quantite }
        assertEquals(2.0, l["SkyPanel"])
        assertEquals(1.0, l["Fresnel 650"])
    }

    @Test
    fun `les charges par circuit`() {
        val plan = PlanSol(1, "a", elements = listOf(projo(1, "SkyPanel", "a"), projo(2, "Fresnel 650", "A"), projo(3, "Inconnu", "b"), projo(4, "SkyPanel")))
        val watts = mapOf("SkyPanel" to 400.0, "Fresnel 650" to 650.0)
        val c = ListeLocation.charges(plan, { watts[it.projecteur] }, nom)
        assertEquals(listOf("A", "B", ListeLocation.SANS_CIRCUIT), c.map { it.circuit })
        assertEquals(1050.0, c[0].puissanceW)
        assertEquals(1, c[1].sansPuissance)
        val prise = Circuit("16 A", Region.EUROPE, 230.0, 16.0, false)
        assertEquals(1050.0 / 230.0, ListeLocation.courant(1050.0, prise), 1e-9)
        assertEquals(1050.0 / 2944.0, ListeLocation.partDeLaMarge(1050.0, prise), 1e-9)
        assertEquals(7.8125, ListeLocation.groupeKva(5000.0), 1e-9)
    }
}
