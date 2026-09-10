package com.rtctek.newsapp.architecture

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rtctek.newsapp.NewsModel
import com.rtctek.newsapp.utils.Constants

/**
 * App database: one table ("articles") holds the cached category feeds and
 * the user's saved stories.
 */
@Database(entities = [NewsModel::class], version = 4, exportSchema = false)
abstract class NewsDatabase : RoomDatabase() {

    abstract fun newsDao(): NewsDao

    companion object {

        @Volatile
        private var INSTANCE: NewsDatabase? = null

        fun getDatabaseClient(context: Context): NewsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NewsDatabase::class.java,
                    Constants.DATABASE_NAME,
                )
                    // The data is a refetchable cache; a destructive migration
                    // is preferable to shipping migration code for a news app.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
