package com.example.moviewather.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.moviewather.data.local.dao.CategoryDao
import com.example.moviewather.data.local.dao.MovieDao
import com.example.moviewather.data.local.entity.Category
import com.example.moviewather.data.local.entity.Movie
import com.example.moviewather.data.local.entity.MovieCategoryCrossRef

/**
 * The single Room database for the application.
 *
 * Version 2 introduces the [Category] and [MovieCategoryCrossRef] entities
 * for Section 7 (Categories & Custom Lists).
 */
@Database(
    entities = [
        Movie::class,
        Category::class,
        MovieCategoryCrossRef::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao
    abstract fun categoryDao(): CategoryDao

    companion object {

        /** Volatile ensures that writes are immediately visible to all threads. */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Migration from version 1 (movies only) to version 2 (categories + junction table). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `dateCreated` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `movie_category_cross_ref` (
                        `movieId` INTEGER NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        PRIMARY KEY(`movieId`, `categoryId`),
                        FOREIGN KEY(`movieId`) REFERENCES `movies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_movie_category_cross_ref_movieId` ON `movie_category_cross_ref` (`movieId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_movie_category_cross_ref_categoryId` ON `movie_category_cross_ref` (`categoryId`)"
                )
            }
        }

        /** Migration from version 2 to version 3 (movie identification & metadata columns). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `releaseYear` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `posterUrl` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `overview` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `genres` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `director` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `mainCast` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `runtime` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `externalId` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `metadataProvider` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `metadataLastUpdated` INTEGER DEFAULT NULL")
            }
        }

        /**
         * Returns the singleton [AppDatabase], creating it on the first call.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moviewatcher.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = ON;")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
