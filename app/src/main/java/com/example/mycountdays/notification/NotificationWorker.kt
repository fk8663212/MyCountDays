package com.example.mycountdays.notification

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.mycountdays.data.AppDatabase
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    
    private val notificationHelper = NotificationHelper(context)
    private val database = AppDatabase.getDatabase(context)
    
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {
        // 獲取所有事件
        val events = runBlocking { database.eventDao().getAllEvents() }
        
        // 始終更新所有常駐通知，確保顯示最新的日期
        for (event in events) {
            // 處理常駐通知 - 每次工作執行時始終更新，確保日期正確
            if (event.showNotification) {
                notificationHelper.showPersistentNotification(event)
            }
            
            // 檢查是否需要發送紀念日通知
            val formatter = DateTimeFormatter.ofPattern("yyyy/M/d")
            val eventDate = try {
                LocalDate.parse(event.date, formatter)
            } catch (ex: Exception) {
                continue
            }
            
            val today = LocalDate.now()
            
            // 檢查是否是100天紀念日
            if (event.notify100Days) {
                val daysBetween = ChronoUnit.DAYS.between(eventDate, today).toInt()
                if (daysBetween == 100) {
                    notificationHelper.showReminderNotification(
                        event,
                        "今天是 ${event.title} 的100天紀念日！"
                    )
                }
            }
            
            // 檢查是否是年度紀念日
            if (event.notify1Year) {
                val startYear = eventDate.year
                val currentYear = today.year
                
                // 檢查是否同一天
                val isSameMonthDay = today.dayOfMonth == eventDate.dayOfMonth && 
                                    today.month == eventDate.month
                
                if (isSameMonthDay && currentYear > startYear) {
                    val years = currentYear - startYear
                    notificationHelper.showReminderNotification(
                        event,
                        "今天是 ${event.title} 的 $years 週年紀念日！"
                    )
                }
            }
        }
        
        // 更新最後更新日期
        notificationHelper.updateLastUpdateDate()
        return Result.success()
    }
}
