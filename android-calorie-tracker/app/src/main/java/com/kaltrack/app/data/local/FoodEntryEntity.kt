package com.kaltrack.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ein Eintrag im Tagebuch: "200 g Magerquark am 19.08.2026".
 *
 * Nährwerte werden bewusst je 100 g gespeichert (und nicht schon ausgerechnet),
 * damit sich die Menge nachträglich ändern lässt.
 */
@Entity(tableName = "food_entries")
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** ISO-Datum, z. B. "2026-08-19". */
    val date: String,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val amountGrams: Double,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val createdAt: Long = System.currentTimeMillis(),
)

val FoodEntryEntity.kcal: Double get() = kcalPer100 * amountGrams / 100.0

val FoodEntryEntity.protein: Double get() = proteinPer100 * amountGrams / 100.0
