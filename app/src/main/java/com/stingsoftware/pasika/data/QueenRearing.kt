package com.stingsoftware.pasika.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.room.ColumnInfo
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

    fun getLabel(context: Context): String {
        return context.getString(labelRes)
    }
}

/**
 * Enum to track the status of a queen cell throughout its lifecycle.
 */
enum class QueenCellStatus(@StringRes val labelRes: Int) {
    GRAFTED(R.string.queen_cell_status_grafted),
    ACCEPTED(R.string.queen_cell_status_accepted),
    CAPPED(R.string.queen_cell_status_capped),
    EMERGED(R.string.queen_cell_status_emerged),
    MATING(R.string.queen_cell_status_mating),
    LAYING(R.string.queen_cell_status_laying),
    SOLD(R.string.queen_cell_status_sold),
    FAILED(R.string.queen_cell_status_failed);

    fun getLabel(context: Context): String {
        return context.getString(labelRes)
    }
}

/**
 * Represents a batch of grafted queen cells.
 */
@Entity(tableName = "grafting_batches")
data class GraftingBatch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val graftingDate: Long,
    val motherHiveId: Long,
    val cellsGrafted: Int,
    @ColumnInfo(defaultValue = "1")
    val useStarterColony: Boolean = true,
    val notes: String? = null
)

/**
 * Represents a single queen cell within a grafting batch.
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

/**
 * Represents a custom task defined by the user for a grafting batch.
 */
@Entity(tableName = "custom_tasks")
data class CustomTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val title: String,
    val daysAfterGrafting: Int
)
