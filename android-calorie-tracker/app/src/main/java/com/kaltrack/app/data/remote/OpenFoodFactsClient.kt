package com.kaltrack.app.data.remote

import com.kaltrack.app.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Minimaler Client für die offene Produktdatenbank
 * [Open Food Facts](https://world.openfoodfacts.org) (Open Database License).
 *
 * Es wird bewusst nur `org.json` zum Parsen benutzt – das ist in Android
 * enthalten, spart eine Serialisierungs-Bibliothek und ist robust gegenüber
 * den ziemlich wechselhaften Feldern der API.
 */
class OpenFoodFactsClient(
    private val http: OkHttpClient = defaultHttpClient(),
    /** Überschreibbar, damit sich der Client gegen einen Testserver prüfen lässt. */
    private val baseUrl: String = DEFAULT_BASE,
) {

    /** Produkt per Barcode (EAN-8/13, UPC ...). Gibt null zurück, wenn unbekannt. */
    suspend fun productByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/v2/product/${barcode.trim()}.json?fields=$FIELDS"
        val body = get(url) ?: return@withContext null
        val root = JSONObject(body)
        if (root.optInt("status", 0) != 1) return@withContext null
        val product = root.optJSONObject("product") ?: return@withContext null
        parseProduct(product, fallbackBarcode = barcode)
    }

    /** Freitextsuche, falls kein Barcode zur Hand ist (loses Obst, Restaurant ...). */
    suspend fun search(query: String, pageSize: Int = 20): List<Product> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl/cgi/search.pl?search_terms=$q&search_simple=1&action=process" +
            "&json=1&page_size=$pageSize&fields=$FIELDS"
        val body = get(url) ?: return@withContext emptyList()
        val products: JSONArray = JSONObject(body).optJSONArray("products")
            ?: return@withContext emptyList()

        buildList {
            for (i in 0 until products.length()) {
                val obj = products.optJSONObject(i) ?: continue
                parseProduct(obj, fallbackBarcode = null)?.let(::add)
            }
        }
    }

    private fun get(url: String): String? {
        val request = Request.Builder()
            .url(url)
            // Open Food Facts verlangt einen aussagekräftigen User-Agent.
            .header("User-Agent", USER_AGENT)
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    private fun parseProduct(obj: JSONObject, fallbackBarcode: String?): Product? {
        val nutriments = obj.optJSONObject("nutriments") ?: JSONObject()

        val kcal = nutriments.readKcalPer100() ?: return null
        val protein = nutriments.readDouble("proteins_100g")
            ?: nutriments.readDouble("proteins")
            ?: 0.0

        val name = obj.firstNonBlank("product_name_de", "product_name", "generic_name_de", "generic_name")
            ?: obj.firstNonBlank("brands")
            ?: return null

        return Product(
            barcode = obj.firstNonBlank("code") ?: fallbackBarcode,
            name = name.trim(),
            brand = obj.firstNonBlank("brands")?.substringBefore(',')?.trim(),
            kcalPer100 = kcal,
            proteinPer100 = protein,
            servingGrams = parseServingSize(obj.firstNonBlank("serving_size")),
        )
    }

    companion object {
        const val DEFAULT_BASE = "https://world.openfoodfacts.org"
        private const val USER_AGENT = "KalTrack/1.0 (Android; https://github.com/GH-TimG/vscode-live-server)"
        private const val FIELDS =
            "code,product_name,product_name_de,generic_name,generic_name_de,brands,serving_size,nutriments"

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}

/**
 * Energie je 100 g. Open Food Facts liefert je nach Datensatz `energy-kcal_100g`,
 * nur `energy-kcal` oder ausschließlich Kilojoule – deshalb die Kaskade.
 */
private fun JSONObject.readKcalPer100(): Double? {
    readDouble("energy-kcal_100g")?.let { return it }
    readDouble("energy-kcal")?.let { return it }

    val energy = readDouble("energy_100g") ?: readDouble("energy") ?: return null
    val unit = optString("energy_unit").ifBlank { "kJ" }
    return if (unit.equals("kcal", ignoreCase = true)) energy else energy / KJ_PER_KCAL
}

private const val KJ_PER_KCAL = 4.184

/** Liest eine Zahl, egal ob die API sie als Number oder als String schickt. */
private fun JSONObject.readDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val direct = optDouble(key, Double.NaN)
    if (!direct.isNaN()) return direct.takeIf { it.isFinite() }
    return optString(key).replace(',', '.').trim().toDoubleOrNull()
}

private fun JSONObject.firstNonBlank(vararg keys: String): String? =
    keys.asSequence()
        .map { optString(it) }
        .firstOrNull { it.isNotBlank() && it != "null" }

/** Wandelt Strings wie "30 g", "1 Riegel (21,5 g)" oder "250ml" in Gramm um. */
internal fun parseServingSize(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    val match = SERVING_REGEX.find(raw.replace(',', '.')) ?: return null
    return match.groupValues[1].toDoubleOrNull()?.takeIf { it > 0.0 }
}

private val SERVING_REGEX = Regex("""(\d+(?:\.\d+)?)\s*(?:g|ml)\b""", RegexOption.IGNORE_CASE)
