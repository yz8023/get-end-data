package com.expiryguard.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 扩展属性，用于创建 DataStore 实例
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings_prefs"
)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供 DataStore 实例，用于存储应用设置偏好
     */
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.settingsDataStore
    }
}