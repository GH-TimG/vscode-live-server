package com.kaltrack.app.data.model

/**
 * Ein Lebensmittel mit Nährwerten je 100 g/ml.
 *
 * Kommt entweder aus Open Food Facts, aus dem lokalen Cache oder aus einer
 * manuellen Eingabe (dann ist [barcode] null).
 */
data class Product(
    val barcode: String?,
    val name: String,
    val brand: String?,
    val kcalPer100: Double,
    val proteinPer100: Double,
    /** Portionsgröße in Gramm, falls Open Food Facts eine kennt. */
    val servingGrams: Double? = null,
)
