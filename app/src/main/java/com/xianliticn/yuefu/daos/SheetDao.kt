package com.xianliticn.yuefu.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.xianliticn.yuefu.entities.Sheet

@Dao
interface SheetDao {
    @Query("SELECT * FROM sheet")
    suspend fun getAll(): List<Sheet>

    @Query("SELECT * FROM sheet ORDER BY last_open_time DESC")
    suspend fun getAllOpenTimeDesc(): List<Sheet>

    @Query("SELECT * FROM sheet WHERE file_name = :fileName")
    suspend fun getByFileName(fileName: String): Sheet?

    @Update
    suspend fun update(sheet: Sheet)

    @Insert
    suspend fun insert(sheet: Sheet)

    @Delete
    suspend fun delete(sheet: Sheet)

    @Query("SELECT * FROM sheet WHERE hash = :hash")
    suspend fun getSameHash(hash: String): List<Sheet>
}