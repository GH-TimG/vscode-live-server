package com.kaltrack.app.data

import com.kaltrack.app.data.local.FoodEntryDao
import com.kaltrack.app.data.local.FoodEntryEntity
import com.kaltrack.app.data.local.ProductDao
import com.kaltrack.app.data.local.ProductEntity
import com.kaltrack.app.data.local.toEntity
import com.kaltrack.app.data.local.toProduct
import com.kaltrack.app.data.model.Product
import com.kaltrack.app.data.remote.OpenFoodFactsClient
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.time.LocalDate

/** Einzige Anlaufstelle der UI für Daten. */
class FoodRepository(
    private val entryDao: FoodEntryDao,
    private val productDao: ProductDao,
    private val off: OpenFoodFactsClient,
) {

    fun observeDay(date: LocalDate): Flow<List<FoodEntryEntity>> =
        entryDao.observeByDate(date.toString())

    fun observeRecentProducts(limit: Int = 25): Flow<List<ProductEntity>> =
        productDao.observeRecent(limit)

    suspend fun addEntry(product: Product, amountGrams: Double, date: LocalDate): Long {
        product.toEntity()?.let { productDao.upsert(it) }
        return entryDao.insert(
            FoodEntryEntity(
                date = date.toString(),
                name = product.name,
                brand = product.brand,
                barcode = product.barcode,
                amountGrams = amountGrams,
                kcalPer100 = product.kcalPer100,
                proteinPer100 = product.proteinPer100,
            ),
        )
    }

    suspend fun deleteEntry(id: Long) = entryDao.deleteById(id)

    /** Für "Rückgängig" nach dem Löschen – die id wird neu vergeben. */
    suspend fun restoreEntry(entry: FoodEntryEntity): Long = entryDao.insert(entry.copy(id = 0L))

    suspend fun findEntry(id: Long): FoodEntryEntity? = entryDao.findById(id)

    /**
     * Barcode auflösen: erst der lokale Cache (schnell und offline), dann
     * Open Food Facts. [LookupResult] unterscheidet bewusst zwischen
     * "nicht gefunden" und "kein Netz" – für den Nutzer sind das zwei sehr
     * verschiedene Situationen.
     */
    suspend fun lookupBarcode(barcode: String): LookupResult {
        productDao.findByBarcode(barcode)?.let { return LookupResult.Found(it.toProduct()) }
        return try {
            val remote = off.productByBarcode(barcode)
            if (remote == null) {
                LookupResult.NotFound
            } else {
                remote.toEntity()?.let { productDao.upsert(it) }
                LookupResult.Found(remote)
            }
        } catch (e: IOException) {
            LookupResult.Offline
        } catch (e: RuntimeException) {
            // Kaputtes JSON o. Ä. – für den Nutzer dasselbe wie "nicht gefunden".
            LookupResult.NotFound
        }
    }

    suspend fun search(query: String): Result<List<Product>> = runCatching {
        off.search(query)
    }

    sealed interface LookupResult {
        data class Found(val product: Product) : LookupResult
        data object NotFound : LookupResult
        data object Offline : LookupResult
    }
}
