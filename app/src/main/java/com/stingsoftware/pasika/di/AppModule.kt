package com.stingsoftware.pasika.di

import android.content.Context
import com.stingsoftware.pasika.data.ApiaryDao
import com.stingsoftware.pasika.data.AppDatabase
import com.stingsoftware.pasika.data.HiveDao
import com.stingsoftware.pasika.data.InspectionDao
import com.stingsoftware.pasika.data.TaskDao
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
}