package com.example.mycountdays

import android.app.Application

class MyApplication : Application() {
    // 臨時存儲選擇的圖片URI
    var tempImageUri: String? = null
    
    // 臨時存儲裁剪後的圖片URI
    var croppedImageUri: String? = null
    
    override fun onCreate() {
        super.onCreate()
        // 初始化工作
    }
}
