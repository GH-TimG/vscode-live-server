package com.kaltrack.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kaltrack.app.data.model.Product

/**
 * Lokaler Cache für gescannte Produkte. Damit funktioniert ein erneuter Scan
 * desselben Barcodes auch ohne Netz.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val brand: String?,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val servingGrams: Double?,
    val lastUsedAt: Long = System.currentTimeMillis(),
)

fun ProductEntity.toProduct(): Product = Product(
    barcode = barcode,
    name = name,
    brand = brand,
    kcalPer100 = kcalPer100,
    proteinPer100 = proteinPer100,
    servingGrams = servingGrams,
)

fun Product.toEntity(lastUsedAt: Long = System.currentTimeMillis()): ProductEntity? {
    val code = barcode ?: return null
    return ProductEntity(
        barcode = code,
        name = name,
        brand = brand,
        kcalPer100 = kcalPer100,
        proteinPer100 = proteinPer100,
        servingGrams = servingGrams,
        lastUsedAt = lastUsedAt,
    )
}
