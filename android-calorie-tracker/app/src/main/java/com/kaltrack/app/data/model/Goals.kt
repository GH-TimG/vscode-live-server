package com.kaltrack.app.data.model

/** Tagesziele. */
data class Goals(
    val kcal: Int = DEFAULT_KCAL,
    val protein: Int = DEFAULT_PROTEIN,
) {
    companion object {
        const val DEFAULT_KCAL = 2000
        const val DEFAULT_PROTEIN = 120
    }
}
