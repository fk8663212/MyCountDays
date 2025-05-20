package com.example.mycountdays.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mycountdays.MainActivity
import com.example.mycountdays.R
import com.example.mycountdays.data.Event
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_PERSISTENT = "persistent_notifications"
        const val CHANNEL_ID_REMINDER = "reminder_notifications"
        private const val PERSISTENT_NOTIFICATION_ID_BASE = 1000
        
        // 新增一個變數來保存上次更新通知的日期
        private var lastUpdateDate = LocalDate.now()
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 創建常駐通知頻道
            val persistentChannel = NotificationChannel(
                CHANNEL_ID_PERSISTENT,
                "常駐通知",
                NotificationManager.IMPORTANCE_LOW // 使用低重要性減少打擾
            ).apply {
                description = "顯示常駐在通知欄的事件"
            }
            
            // 創建提醒通知頻道
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "重要日期提醒",
                NotificationManager.IMPORTANCE_HIGH // 使用高重要性確保提醒被看到
            ).apply {
                description = "重要日期和紀念日提醒"
            }
            
            // 註冊通知頻道
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(persistentChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    // 檢查是否擁有發送通知的權限
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13 以前的版本不需要明確請求通知權限
            true
        }
    }

    // 顯示常駐通知
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showPersistentNotification(event: Event) {
        // 先檢查權限
        if (!hasNotificationPermission()) {
            return  // 如果沒有權限，直接返回
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("EVENT_ID", event.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            event.id, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // 計算剩餘天數
        val dayCountText = calculateDayDisplay(event)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PERSISTENT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(event.title)
            .setContentText(dayCountText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 讓通知常駐
            .setContentIntent(pendingIntent)
            // 確保通知不會被系統的通知摘要合併
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .build()
            
        NotificationManagerCompat.from(context).notify(PERSISTENT_NOTIFICATION_ID_BASE + event.id, notification)
    }
    
    // 移除常駐通知
    fun removePersistentNotification(eventId: Int) {
        NotificationManagerCompat.from(context).cancel(PERSISTENT_NOTIFICATION_ID_BASE + eventId)
    }
    
    // 顯示紀念日提醒通知
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showReminderNotification(event: Event, message: String) {
        // 先檢查權限
        if (!hasNotificationPermission()) {
            return  // 如果沒有權限，直接返回
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("EVENT_ID", event.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            event.id, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(event.title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // 點擊後自動移除
            .build()
            
        NotificationManagerCompat.from(context).notify(event.id, notification)
    }
    
    // 計算事件顯示文字
    private fun calculateDayDisplay(event: Event): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy/M/d")
        val eventDate = try {
            LocalDate.parse(event.date, formatter)
        } catch (ex: Exception) {
            return event.date
        }
        
        val today = LocalDate.now()
        val diff = ChronoUnit.DAYS.between(today, eventDate).toInt()
        
        return when (event.category) {
            "D-DAY" -> {
                if (diff > 0) "還有 $diff 天" else if (diff < 0) "已過 ${-diff} 天" else "就是今天！"
            }
            "紀念日" -> {
                if (diff <= 0) {
                    val passedDays = -diff + 1  // 加1，讓紀念日當天算作第1天
                    "已經 $passedDays 天"
                } else {
                    "還沒開始"
                }
            }
            "每年" -> {
                // 計算今年的紀念日
                val thisYearDate = LocalDate.of(today.year, eventDate.month, eventDate.dayOfMonth)
                val daysToThisYear = ChronoUnit.DAYS.between(today, thisYearDate).toInt()
                
                if (daysToThisYear > 0) {
                    "今年還有 $daysToThisYear 天"
                } else if (daysToThisYear < 0) {
                    val nextYearDate = LocalDate.of(today.year + 1, eventDate.month, eventDate.dayOfMonth)
                    val daysToNextYear = ChronoUnit.DAYS.between(today, nextYearDate).toInt()
                    "明年還有 $daysToNextYear 天"
                } else {
                    "就是今天！"
                }
            }
            "每月" -> {
                // 計算本月的紀念日
                val thisMonthDate = LocalDate.of(today.year, today.month, eventDate.dayOfMonth)
                val daysToThisMonth = ChronoUnit.DAYS.between(today, thisMonthDate).toInt()
                
                if (daysToThisMonth > 0) {
                    "本月還有 $daysToThisMonth 天"
                } else if (daysToThisMonth < 0) {
                    val nextMonthDate = thisMonthDate.plusMonths(1)
                    val daysToNextMonth = ChronoUnit.DAYS.between(today, nextMonthDate).toInt()
                    "下月還有 $daysToNextMonth 天"
                } else {
                    "就是今天！"
                }
            }
            else -> {
                if (diff > 0) "還有 $diff 天" else if (diff < 0) "已過 ${-diff} 天" else "就是今天！"
            }
        }
    }

    /**
     * 檢查是否需要更新所有通知（當日期變更時）
     * 如果今天的日期與上次更新日期不同，則返回true
     */
    fun shouldUpdateNotifications(): Boolean {
        val today = LocalDate.now()
        return !today.equals(lastUpdateDate)
    }

    /**
     * 更新最後更新日期為今天
     */
    fun updateLastUpdateDate() {
        lastUpdateDate = LocalDate.now()
    }
}
