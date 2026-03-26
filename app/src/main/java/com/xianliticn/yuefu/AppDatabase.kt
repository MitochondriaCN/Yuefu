package com.xianliticn.yuefu

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.xianliticn.yuefu.daos.SheetDao
import com.xianliticn.yuefu.entities.Sheet

@Database(
    entities = [Sheet::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sheetDao(): SheetDao
}