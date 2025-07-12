// app/src/main/java/com/stingsoftware/pasika/di/AppModule.kt
package com.stingsoftware.pasika.di

import android.content.Context
import com.stingsoftware.pasika.data.AppDatabase
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
    fun provideApiaryRepository(db: AppDatabase): ApiaryRepository {
        return ApiaryRepository(db.apiaryDao(), db.hiveDao(), db.inspectionDao())
    }
}