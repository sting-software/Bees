package com.stingsoftware.pasika.data

data class QueenRearingStats(
    val totalCells: Int = 0,
    val acceptedCells: Int = 0,
    val emergedQueens: Int = 0,
    val layingQueens: Int = 0,
    val acceptanceRate: Float = 0f,
    val emergenceRate: Float = 0f,
    val matingSuccessRate: Float = 0f
)