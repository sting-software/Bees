package com.stingsoftware.pasika.data

/**
 * Represents the performance of a single stage in the queen rearing process.
 *
 * @property startingCount The number of cells that entered this stage.
 * @property successCount The number of cells that successfully completed this stage.
 * @property successRate The percentage of cells that were successful.
 */
data class StagePerformance(
    val startingCount: Int = 0,
    val successCount: Int = 0,
    val successRate: Float = 0f // Calculated as (successCount / startingCount) * 100
)

/**
 * A comprehensive data container for all queen rearing analytics.
 * It breaks down the process into a funnel of stages and provides overall statistics.
 *
 * @property totalGrafted The initial number of cells grafted.
 * @property acceptance Performance from Grafted to Accepted.
 * @property capping Performance from Accepted to Capped.
 * @property emergence Performance from Capped to Emerged.
 * @property mating Performance from Emerged to Laying/Sold.
 * @property overallSuccessRate The final percentage of laying queens from the initial number of grafted cells.
 * @property cellStatusDistribution A map showing the count of cells in each status.
 */
data class QueenRearingAnalytics(
    val totalGrafted: Int = 0,
    val acceptance: StagePerformance = StagePerformance(),
    val capping: StagePerformance = StagePerformance(),
    val emergence: StagePerformance = StagePerformance(),
    val mating: StagePerformance = StagePerformance(),
    val overallSuccessRate: Float = 0f,
    val cellStatusDistribution: Map<QueenCellStatus, Int> = emptyMap()
)
