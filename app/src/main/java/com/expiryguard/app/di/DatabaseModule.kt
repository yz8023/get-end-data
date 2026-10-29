package com.expiryguard.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expiryguard.app.data.db.AppDatabase
import com.expiryguard.app.data.db.dao.CategoryDao
import com.expiryguard.app.data.db.dao.ProductDao
import com.expiryguard.app.data.db.dao.ShelfLifeGroupDao
import com.expiryguard.app.data.repository.ProductRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // 同步执行，保证首次读取前默认数据已经可见。
                    AppDatabase.seedDefaultData(db)
                }
            })
            .build()
    }

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideShelfLifeGroupDao(db: AppDatabase): ShelfLifeGroupDao = db.shelfLifeGroupDao()

    @Provides
    @Singleton
    fun provideProductRepository(
        @ApplicationContext context: Context,
        db: AppDatabase,
        productDao: ProductDao,
        categoryDao: CategoryDao,
        shelfLifeGroupDao: ShelfLifeGroupDao
    ): ProductRepository {
        return ProductRepository(context, db, productDao, categoryDao, shelfLifeGroupDao)
    }
}
