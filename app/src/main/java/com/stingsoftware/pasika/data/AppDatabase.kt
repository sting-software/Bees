package com.stingsoftware.pasika.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Apiary::class,
        Hive::class,
        Inspection::class,
        Task::class,
        GraftingBatch::class,
        QueenCell::class,
        CustomTask::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun apiaryDao(): ApiaryDao
    abstract fun hiveDao(): HiveDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun taskDao(): TaskDao
    abstract fun queenRearingDao(): QueenRearingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `dueDate` INTEGER,
                        `isCompleted` INTEGER NOT NULL,
                        `reminderEnabled` INTEGER NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX `index_hives_apiaryId` ON `hives` (`apiaryId`)")
                db.execSQL("CREATE INDEX `index_inspections_hiveId` ON `inspections` (`hiveId`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `apiaries` ADD COLUMN `displayOrder` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `hives` ADD COLUMN `role` TEXT NOT NULL DEFAULT 'PRODUCTION'")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `graftingBatchId` INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `grafting_batches` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `graftingDate` INTEGER NOT NULL, 
                        `motherHiveId` INTEGER NOT NULL, 
                        `cellsGrafted` INTEGER NOT NULL, 
                        `notes` TEXT
                    )
                """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `queen_cells` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `batchId` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `starterHiveId` INTEGER, 
                        `finisherHiveId` INTEGER, 
                        `nucleusHiveId` INTEGER, 
                        `dateMovedToFinisher` INTEGER, 
                        `dateEmerged` INTEGER, 
                        `dateMovedToNucleus` INTEGER, 
                        `dateStartedLaying` INTEGER
                    )
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `grafting_batches` ADD COLUMN `useStarterColony` INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `batchId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `daysAfterGrafting` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- Refactor Hives Table ---
                db.execSQL("""
                    CREATE TABLE hives_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        apiaryId INTEGER NOT NULL,
                        hiveNumber TEXT,
                        hiveType TEXT,
                        hiveTypeOther TEXT,
                        frameType TEXT,
                        frameTypeOther TEXT,
                        material TEXT,
                        materialOther TEXT,
                        breed TEXT,
                        breedOther TEXT,
                        notes TEXT,
                        role TEXT NOT NULL DEFAULT 'PRODUCTION',
                        queenTagColor TEXT,
                        queenTagColorOther TEXT,
                        queenNumber TEXT,
                        queenYear TEXT,
                        queenLine TEXT,
                        isolationFromDate INTEGER,
                        isolationToDate INTEGER,
                        FOREIGN KEY(apiaryId) REFERENCES apiaries(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO hives_new (id, apiaryId, hiveNumber, hiveType, hiveTypeOther, frameType, frameTypeOther, material, materialOther, breed, breedOther, notes, role, queenTagColor, queenTagColorOther, queenNumber, queenYear, queenLine, isolationFromDate, isolationToDate)
                    SELECT id, apiaryId, hiveNumber, hiveType, hiveTypeOther, frameType, frameTypeOther, material, materialOther, breed, breedOther, notes, role, queenTagColor, queenTagColorOther, queenNumber, queenYear, queenLine, isolationFromDate, isolationToDate FROM hives
                """.trimIndent())
                db.execSQL("DROP TABLE hives")
                db.execSQL("ALTER TABLE hives_new RENAME TO hives")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hives_apiaryId` ON `hives` (`apiaryId`)")

                // --- Refactor Inspections Table ---
                db.execSQL("""
                    CREATE TABLE inspections_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hiveId INTEGER NOT NULL,
                        inspectionDate INTEGER NOT NULL DEFAULT 0,
                        queenCellsPresent INTEGER,
                        queenCellsCount INTEGER,
                        framesEggsCount INTEGER,
                        framesOpenBroodCount INTEGER,
                        framesCappedBroodCount INTEGER,
                        framesHoneyCount INTEGER,
                        framesPollenCount INTEGER,
                        pestsDiseasesObserved TEXT,
                        treatment TEXT,
                        defensivenessRating INTEGER,
                        managementActionsTaken TEXT,
                        givenBuiltCombs INTEGER,
                        givenFoundation INTEGER,
                        givenBrood INTEGER,
                        givenBeesKg REAL,
                        givenHoneyKg REAL,
                        givenSugarKg REAL,
                        notes TEXT,
                        FOREIGN KEY(hiveId) REFERENCES hives(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO inspections_new (id, hiveId, inspectionDate, queenCellsPresent, queenCellsCount, framesEggsCount, framesOpenBroodCount, framesCappedBroodCount, framesHoneyCount, framesPollenCount, pestsDiseasesObserved, treatment, defensivenessRating, managementActionsTaken, notes)
                    SELECT id, hiveId, inspectionDate, queenCellsPresent, queenCellsCount, framesEggsCount, framesOpenBroodCount, framesCappedBroodCount, honeyStoresEstimateFrames, pollenStoresEstimateFrames, pestsDiseasesObserved, treatmentApplied, temperamentRating, managementActionsTaken, notes FROM inspections
                """.trimIndent())
                db.execSQL("DROP TABLE inspections")
                db.execSQL("ALTER TABLE inspections_new RENAME TO inspections")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inspections_hiveId` ON `inspections` (`hiveId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pasika_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
