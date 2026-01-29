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

    /**
     * 获取未下载的乐谱，按创建时间降序排序
     */
    @Query("SELECT * FROM sheet WHERE is_downloaded = 0 ORDER BY create_time DESC")
    suspend fun getAllNotDownloaded(): List<Sheet>

    /**
     * 获取已下载的乐谱，按最后打开时间降序排序
     */
    @Query("SELECT * FROM sheet WHERE is_downloaded = 1 ORDER BY last_open_time DESC")
    suspend fun getAllDownloaded(): List<Sheet>

    @Query("SELECT * FROM sheet WHERE file_name = :fileName")
    suspend fun getByFileName(fileName: String): Sheet?

    @Query("SELECT * FROM sheet WHERE id = :id")
    suspend fun getById(id: Int): Sheet?

    @Update
    suspend fun update(sheet: Sheet)

    @Insert
    suspend fun insert(sheet: Sheet)

    @Delete
    suspend fun delete(sheet: Sheet)

    @Query("SELECT * FROM sheet WHERE hash = :hash")
    suspend fun getSameHash(hash: String): List<Sheet>

    @Query("SELECT * FROM sheet WHERE file_name LIKE '%' || :fileName || '%' AND is_downloaded = 1")
    suspend fun getLikeFileName(fileName: String): List<Sheet>
}