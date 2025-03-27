package com.example.mycountdays.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,      // 標題
    val date: String,         // 日期，例如 "2023-10-20"
    val imageUri: String?,   // 圖片URL
    val category: String?,     //類別

    val showNotification: Boolean = false,
    val notify100Days: Boolean = true,
    val notify1Year: Boolean = true,
)