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
    version = 9, // Incremented version from 8 to 9
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


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pasika_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
