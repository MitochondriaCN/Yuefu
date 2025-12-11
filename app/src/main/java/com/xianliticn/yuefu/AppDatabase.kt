package com.xianliticn.yuefu

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xianliticn.yuefu.daos.SheetDao
import com.xianliticn.yuefu.entities.Sheet

@Database(entities = [Sheet::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sheetDao(): SheetDao
}