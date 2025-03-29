package com.example.mycountdays.data

import android.app.Application

class EventApplication: Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}