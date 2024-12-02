package com.lion.a02_team_6_shoppingmall.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BookVO::class], version = 1, exportSchema = true)
abstract class BookDatabase : RoomDatabase(){
    abstract fun bookDAO(): BookDAO

    companion object{
        var bookDatabase:BookDatabase? = null
        @Synchronized
        fun getInstance(context: Context) : BookDatabase?{
            synchronized(BookDatabase::class){
                bookDatabase = Room.databaseBuilder(
                    context.applicationContext, BookDatabase::class.java,
                    "Book.db"
                )
                .build()
            }
            return bookDatabase
        }

        fun destroyInstance(){
            bookDatabase = null
        }
    }
}