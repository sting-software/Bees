package com.stingsoftware.pasika.di

import android.content.Context
import com.stingsoftware.pasika.data.*
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the singleton instance of the AppDatabase.
     * This is the ONLY function that should provide the database.
     * It uses the @ApplicationContext provided by Hilt and correctly calls your
     * existing AppDatabase.getDatabase() method.
     */
    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    // --- DAO Providers ---
    // These functions tell Hilt how to provide each individual DAO,
    // which are then injected into ApiaryRepository.

    @Singleton
    @Provides
    fun provideApiaryDao(db: AppDatabase): ApiaryDao {
        return db.apiaryDao()
    }

    @Singleton
    @Provides
    fun provideHiveDao(db: AppDatabase): HiveDao {
        return db.hiveDao()
    }

    @Singleton
    @Provides
    fun provideInspectionDao(db: AppDatabase): InspectionDao {
        return db.inspectionDao()
    }

    @Singleton
    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao {
        return db.taskDao()
    }

    /**
     * Provides the new QueenRearingDao.
     */
    @Singleton
    @Provides
    fun provideQueenRearingDao(db: AppDatabase): QueenRearingDao {
        return db.queenRearingDao()
    }
}
