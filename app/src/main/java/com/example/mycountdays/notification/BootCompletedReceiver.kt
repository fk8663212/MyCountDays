package com.example.mycountdays.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mycountdays.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 設備重啟後重新啟動通知服務的廣播接收器
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "設備重啟，重新啟動通知服務")
            
            // 重新設置工作管理器
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                6, TimeUnit.HOURS
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_notification_check",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            // 恢復所有常駐通知
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val events = database.eventDao().getAllEvents()
                    val notificationHelper = NotificationHelper(context)
                    
                    events.forEach { event ->
                        if (event.showNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            try {
                                notificationHelper.showPersistentNotification(event)
                            } catch (e: Exception) {
                                Log.e("BootCompletedReceiver", "顯示通知失敗: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootCompletedReceiver", "恢復通知時發生錯誤: ${e.message}")
                }
            }
        }
    }
}
