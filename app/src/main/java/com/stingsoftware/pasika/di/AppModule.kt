package com.stingsoftware.pasika.di

import android.content.Context
import com.stingsoftware.pasika.data.AppDatabase
import com.stingsoftware.pasika.data.TaskDao
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

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao {
        return db.taskDao()
    }

    // This is the single, correct function for providing the repository.
    // The outdated one has been removed.
    @Singleton
    @Provides
    fun provideApiaryRepository(db: AppDatabase, taskDao: TaskDao): ApiaryRepository {
        return ApiaryRepository(db.apiaryDao(), db.hiveDao(), db.inspectionDao(), taskDao)
    }
}