package com.stingsoftware.pasika.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stingsoftware.pasika.R

// Enum to define the role of a hive in the queen rearing process.
enum class HiveRole(@StringRes val labelRes: Int) {
    PRODUCTION(R.string.hive_role_production),
    MOTHER(R.string.hive_role_mother),
    STARTER(R.string.hive_role_starter),
    FINISHER(R.string.hive_role_finisher),
    NUCLEUS(R.string.hive_role_nucleus);

    /**
     * A helper function to get the localized string directly from the enum.
     * @param context The context needed to access string resources.
     * @return The localized string for the enum constant.
     */
    fun getLabel(context: Context): String {
        return context.getString(labelRes)
    }
}

/**
 * Enum to track the status of a queen cell throughout its lifecycle.
 * Each status is associated with a string resource for its display name.
 */
enum class QueenCellStatus(@StringRes val labelRes: Int) {
    GRAFTED(R.string.queen_cell_status_grafted),
    ACCEPTED(R.string.queen_cell_status_accepted),
    CAPPED(R.string.queen_cell_status_capped),
    EMERGED(R.string.queen_cell_status_emerged),
    MATING(R.string.queen_cell_status_mating),
    LAYING(R.string.queen_cell_status_laying),
    FAILED(R.string.queen_cell_status_failed);

    /**
     * A helper function to get the localized string directly from the enum.
     * @param context The context needed to access string resources.
     * @return The localized string for the enum constant.
     */
    fun getLabel(context: Context): String {
        return context.getString(labelRes)
    }
}

/**
 * Represents a batch of grafted queen cells. This is the central entity for a queen rearing session.
 * @param id Unique identifier for the batch.
 * @param name A user-defined name for the batch (e.g., "Batch 2025-07-A").
 * @param graftingDate The timestamp when the grafting was performed.
 * @param motherHiveId The ID of the hive from which the larvae were taken.
 * @param cellsGrafted The initial number of cells grafted in this batch.
 * @param notes Any additional notes or comments about the batch.
 */
@Entity(tableName = "grafting_batches")
data class GraftingBatch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val graftingDate: Long,
    val motherHiveId: Long,
    val cellsGrafted: Int,
    val notes: String? = null
)

/**
 * Represents a single queen cell within a grafting batch.
 * @param id Unique identifier for the cell.
 * @param batchId Foreign key linking this cell to a GraftingBatch.
 * @param status The current lifecycle status of the queen cell.
 * @param starterHiveId The ID of the starter hive where the cell was initially placed.
 * @param finisherHiveId The ID of the finisher hive where the cell matured.
 * @param nucleusHiveId The ID of the nucleus hive where the emerged queen is mating.
 * @param dateMovedToFinisher Timestamp when the cell was moved to the finisher.
 * @param dateEmerged Timestamp when the queen emerged from the cell.
 * @param dateMovedToNucleus Timestamp when the queen was moved to the nucleus.
 * @param dateStartedLaying Timestamp when the queen started laying eggs.
 */
@Entity(tableName = "queen_cells")
data class QueenCell(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    var status: QueenCellStatus = QueenCellStatus.GRAFTED,
    var starterHiveId: Long? = null,
    var finisherHiveId: Long? = null,
    var nucleusHiveId: Long? = null,
    var dateMovedToFinisher: Long? = null,
    var dateEmerged: Long? = null,
    var dateMovedToNucleus: Long? = null,
    var dateStartedLaying: Long? = null
)
