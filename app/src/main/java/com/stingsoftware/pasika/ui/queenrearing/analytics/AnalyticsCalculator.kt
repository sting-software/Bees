package com.stingsoftware.pasika.ui.queenrearing.analytics

import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.data.QueenCell
import com.stingsoftware.pasika.data.QueenCellStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Detailed metrics for a single grafting batch.
 */
data class BatchMetrics(
    val batchName: String,
    val totalGrafted: Int,
    val accepted: Int,
    val emerged: Int,
    val laidOrSold: Int,

    // Stage‐to‐stage rates
    val acceptanceRate: Float,
    val emergenceRate: Float,
    val matingSuccessRate: Float,

    // End‐to‐end cumulative yields
    val cumulativeEmergenceRate: Float,
    val cumulativeMatingRate: Float,

    // 95% Wilson‐score confidence intervals (percent‐formatted)
    val acceptanceCI: Pair<Float, Float>?,
    val emergenceCI: Pair<Float, Float>?,
    val matingCI: Pair<Float, Float>?
)

/**
 * Performs progressive, smoothed, and confidence‐interval‐aware
 * analytics for queen‐rearing batches.
 */
@Singleton
class AnalyticsCalculator @Inject constructor() {

    /**
     * Computes a full set of metrics for the given batch.
     *
     * @param batch The grafting batch (contains id, name, cellsGrafted).
     * @param allCells All queen cells; we'll filter those matching this batch.
     * @param smoothingAlpha Laplace smoothing constant (α).
     * @param ciZ Z‐score for confidence intervals (1.96 for 95% CI).
     */
    fun computeBatchMetrics(
        batch: GraftingBatch,
        allCells: List<QueenCell>,
        smoothingAlpha: Float = 1f,
        ciZ: Float = 1.96f
    ): BatchMetrics {
        // 1) Filter cells once
        val cells = allCells.filter { it.batchId == batch.id }
        val grafted = batch.cellsGrafted

        // 2) Raw counts
        val accepted   = cells.count { it.status == QueenCellStatus.ACCEPTED }
        val emerged    = cells.count { it.status == QueenCellStatus.EMERGED }
        val laidOrSold = cells.count {
            it.status == QueenCellStatus.LAYING || it.status == QueenCellStatus.SOLD
        }

        // 3) Raw rates
        val accRaw = if (grafted > 0)   accepted.toFloat()   / grafted else 0f
        val emgRaw = if (accepted > 0)  emerged.toFloat()    / accepted else 0f
        val matRaw = if (emerged > 0)   laidOrSold.toFloat() / emerged else 0f

        // 4) Laplace (add‐α) smoothing
        fun smooth(k: Int, n: Int): Float =
            if (n + 2 * smoothingAlpha > 0f)
                (k + smoothingAlpha) / (n + 2 * smoothingAlpha)
            else 0f

        val accSm = smooth(accepted, grafted)
        val emgSm = smooth(emerged, accepted)
        val matSm = smooth(laidOrSold, emerged)

        // 5) Cumulative yields (raw & smoothed)
        val cumEmgRaw = accRaw * emgRaw
        val cumMatRaw = cumEmgRaw * matRaw

        val cumEmgSm  = accSm  * emgSm
        val cumMatSm  = cumEmgSm * matSm

        // 6) Wilson‐score confidence intervals
        fun binomialCI(k: Int, n: Int): Pair<Float, Float> {
            if (n == 0) return 0f to 0f
            val pHat = k.toFloat() / n
            val z2   = ciZ * ciZ
            val centre = pHat + z2 / (2 * n)
            val half   = ciZ * sqrt((pHat * (1 - pHat) + z2 / (4 * n)) / n)
            val denom  = 1 + z2 / n
            val lower  = ((centre - half) / denom).coerceIn(0f, 1f)
            val upper  = ((centre + half) / denom).coerceIn(0f, 1f)
            return lower to upper
        }

        val accCI = binomialCI(accepted, grafted)
        val emgCI = binomialCI(emerged, accepted)
        val matCI = binomialCI(laidOrSold, emerged)

        // 7) Package into a percent‐based BatchMetrics object
        return BatchMetrics(
            batchName               = batch.name,
            totalGrafted            = grafted,
            accepted                = accepted,
            emerged                 = emerged,
            laidOrSold              = laidOrSold,

            acceptanceRate          = accRaw * 100f,
            emergenceRate           = emgRaw * 100f,
            matingSuccessRate       = matRaw * 100f,

            cumulativeEmergenceRate = cumEmgRaw * 100f,
            cumulativeMatingRate    = cumMatRaw * 100f,

            acceptanceCI            = accCI.first  * 100f to accCI.second  * 100f,
            emergenceCI             = emgCI.first  * 100f to emgCI.second  * 100f,
            matingCI                = matCI.first  * 100f to matCI.second  * 100f
        )
    }
}
