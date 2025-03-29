package com.example.mycountdays

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mycountdays.data.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: Event)

    @Delete
    suspend fun delete(event: Event)

    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<Event>
}