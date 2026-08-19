package com.kaltrack.app.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Die Nährwertfelder von Open Food Facts sind erstaunlich uneinheitlich –
 * mal Zahl, mal String, mal nur Kilojoule. Diese Tests fixieren das Verhalten
 * anhand echter Antwortformen.
 */
class OpenFoodFactsClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OpenFoodFactsClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OpenFoodFactsClient(baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueue(body: String) {
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
    }

    @Test
    fun `parses a normal product and prefers the German name`() = runTest {
        enqueue(
            """
            {"status":1,"code":"4008400202037","product":{
              "code":"4008400202037",
              "product_name":"Duplo",
              "product_name_de":"Duplo Riegel",
              "brands":"Ferrero, Duplo",
              "serving_size":"1 Riegel (18,2 g)",
              "nutriments":{"energy-kcal_100g":543,"proteins_100g":7.4,"energy_100g":2270}
            }}
            """.trimIndent(),
        )

        val product = client.productByBarcode("4008400202037")
        assertNotNull(product)
        requireNotNull(product)
        assertEquals("Duplo Riegel", product.name)
        assertEquals("Ferrero", product.brand)
        assertEquals(543.0, product.kcalPer100, 0.001)
        assertEquals(7.4, product.proteinPer100, 0.001)
        assertEquals(18.2, product.servingGrams!!, 0.001)
        assertEquals("4008400202037", product.barcode)
    }

    @Test
    fun `converts kilojoule when no kcal field is present`() = runTest {
        enqueue(
            """
            {"status":1,"product":{"code":"123","product_name":"Nur kJ",
             "nutriments":{"energy_100g":1000,"energy_unit":"kJ","proteins_100g":5}}}
            """.trimIndent(),
        )

        val product = requireNotNull(client.productByBarcode("123"))
        assertEquals(239.0, product.kcalPer100, 0.5)
    }

    @Test
    fun `reads numbers that arrive as strings`() = runTest {
        enqueue(
            """
            {"status":1,"product":{"code":"123","product_name":"String-Werte",
             "nutriments":{"energy-kcal_100g":"389","proteins_100g":"13,5"}}}
            """.trimIndent(),
        )

        val product = requireNotNull(client.productByBarcode("123"))
        assertEquals(389.0, product.kcalPer100, 0.001)
        assertEquals(13.5, product.proteinPer100, 0.001)
    }

    @Test
    fun `defaults protein to zero when the field is missing`() = runTest {
        enqueue(
            """{"status":1,"product":{"code":"1","product_name":"Öl","nutriments":{"energy-kcal_100g":900}}}""",
        )

        val product = requireNotNull(client.productByBarcode("1"))
        assertEquals(0.0, product.proteinPer100, 0.001)
        assertNull(product.servingGrams)
    }

    @Test
    fun `returns null for an unknown barcode`() = runTest {
        enqueue("""{"status":0,"status_verbose":"product not found"}""")
        assertNull(client.productByBarcode("0000000000000"))
    }

    @Test
    fun `returns null when the product has no usable energy value`() = runTest {
        enqueue("""{"status":1,"product":{"code":"1","product_name":"Leer","nutriments":{}}}""")
        assertNull(client.productByBarcode("1"))
    }

    @Test
    fun `search skips unusable entries and keeps the rest`() = runTest {
        enqueue(
            """
            {"count":2,"products":[
              {"code":"1","product_name":"Magerquark","brands":"Milbona",
               "nutriments":{"energy-kcal_100g":67,"proteins_100g":12}},
              {"code":"2","product_name":"Kaputt","nutriments":{}},
              {"code":"3","product_name_de":"Skyr","nutriments":{"energy-kcal_100g":63,"proteins_100g":11}}
            ]}
            """.trimIndent(),
        )

        val results = client.search("quark")
        assertEquals(listOf("Magerquark", "Skyr"), results.map { it.name })
        assertEquals(12.0, results[0].proteinPer100, 0.001)
    }

    @Test
    fun `search url-encodes the query`() = runTest {
        enqueue("""{"products":[]}""")
        client.search("käse brötchen")
        val path = server.takeRequest().path.orEmpty()
        assertTrue("unerwarteter Pfad: $path", path.contains("search_terms=k%C3%A4se+br%C3%B6tchen"))
    }

    @Test
    fun `serving size parsing handles the common notations`() {
        assertEquals(30.0, parseServingSize("30 g")!!, 0.001)
        assertEquals(250.0, parseServingSize("250ml")!!, 0.001)
        assertEquals(21.5, parseServingSize("1 Riegel (21,5 g)")!!, 0.001)
        assertEquals(125.0, parseServingSize("125 g (1 Becher)")!!, 0.001)
        assertNull(parseServingSize("1 Stück"))
        assertNull(parseServingSize(null))
        assertNull(parseServingSize(""))
    }
}
