package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<HistoryEntity>)

    @Query("SELECT * FROM history ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedHistory(limit: Int, offset: Int): List<HistoryEntity>

    @Query("SELECT * FROM history ORDER BY id DESC")
    fun observeAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY id DESC LIMIT 500")
    suspend fun getAllHistory(): List<HistoryEntity>

    @Query("SELECT COUNT(*) FROM history")
    suspend fun getCount(): Int
}
