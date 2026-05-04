package com.smasharena.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourtDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(courts: List<Court>)

    @Query("SELECT COUNT(*) FROM courts")
    suspend fun count(): Int

    @Query("SELECT * FROM courts ORDER BY name ASC")
    fun observeAll(): Flow<List<Court>>

    @Query("SELECT * FROM courts WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Court?
}
