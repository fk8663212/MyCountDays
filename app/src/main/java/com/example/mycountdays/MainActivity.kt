package com.example.mycountdays

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.rememberAsyncImagePainter
import com.example.mycountdays.data.AppDatabase
import com.example.mycountdays.data.Event
import com.example.mycountdays.notification.NotificationHelper
import com.example.mycountdays.notification.NotificationWorker
import com.example.mycountdays.screen.AddEventScreen
import com.example.mycountdays.ui.theme.MyCountDaysTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Switch
import com.example.mycountdays.screen.SelectEventTypeScreen
import androidx.navigation.navArgument
import com.example.mycountdays.screen.DetailScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var notificationHelper: NotificationHelper

    // 權限請求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 獲得權限後更新常駐通知
            updatePersistentNotifications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        database = AppDatabase.getDatabase(this)
        notificationHelper = NotificationHelper(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 檢查並請求通知權限
        checkNotificationPermission()

        // 設置定期工作來檢查通知
        setupNotificationWorker()

        setContent {
            // 使用 remember 建立 mutableState 保存事件清單
            val saveEvent = remember { mutableStateOf<List<Event>>(emptyList()) }
            val lifecycleOwner = LocalLifecycleOwner.current
            val context = LocalContext.current

            // 初始讀取資料庫
            LaunchedEffect(lifecycleOwner) {
                saveEvent.value = database.eventDao().getAllEvents()
                Log.d("MainActivity", "Initial load: ${saveEvent.value}")
            }

            MyCountDaysTheme {
                val navController = rememberNavController()

                // 監聽返回 home 時重新讀取資料
                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { backStackEntry ->
                        if (backStackEntry.destination.route == "home") {
                            // 在 IO 協程中讀取資料庫
                            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                val events = database.eventDao().getAllEvents()
                                // 回到主線程更新狀態
                                launch(Dispatchers.Main) {
                                    saveEvent.value = events
                                    Log.d("MainActivity", "Reload on home: ${saveEvent.value}")
                                }
                            }
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("selectEventTypeScreen") {
                        SelectEventTypeScreen(navController)
                    }
                    composable(
                        route = "addEventScreen?option={option}",
                        arguments = listOf(
                            navArgument("option") {
                                type = androidx.navigation.NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { backStackEntry ->
                        AddEventScreen(navController, backStackEntry.arguments?.getString("option"))
                    }
                    composable("home") {
                        Greeting(navController, saveEvent = saveEvent)
                    }
                    composable(
                        route = "detailScreen/{eventId}",
                        arguments = listOf(
                            navArgument("eventId") {
                                type = androidx.navigation.NavType.IntType
                            }
                        )
                    ) { backStackEntry ->
                        val eventId = backStackEntry.arguments?.getInt("eventId") ?: return@composable
                        // 根據 ID 找到事件
                        val event = saveEvent.value.find { it.id == eventId }
                        if (event != null) {
                            DetailScreen(event = event, navController = navController)
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 已有權限，更新常駐通知
                    updatePersistentNotifications()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // 可以顯示一個對話框解釋為什麼需要通知權限
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // 直接請求權限
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // 舊版本不需要請求權限
            updatePersistentNotifications()
        }
    }

    private fun updatePersistentNotifications() {
        lifecycleScope.launch(Dispatchers.IO) {
            val events = database.eventDao().getAllEvents()

            for (event in events) {
                if (event.showNotification) {
                    launch(Dispatchers.Main) {
                        notificationHelper.showPersistentNotification(event)
                    }
                }
            }
        }
    }

    private fun setupNotificationWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_notification_check",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    // 當事件被新增或更新時調用此方法
    fun handleEventNotifications(event: Event) {
        if (event.showNotification) {
            notificationHelper.showPersistentNotification(event)
        } else {
            notificationHelper.removePersistentNotification(event.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(navController: NavController, saveEvent: MutableState<List<Event>>) {
    val context = LocalContext.current
    // 取得資料庫實例
    val database = remember { AppDatabase.getDatabase(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "My Count Days") },
                navigationIcon = { /* 可加入返回圖示 */ }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate("selectEventTypeScreen")
            }) {
                Icon(Icons.Default.Add, contentDescription = "新增事件")
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 使用 ReorderableEventList 顯示可排序列表
            ReorderableEventList(
                events = saveEvent.value, 
                onMove = { from, to ->
                    // 1. 先更新本地狀態
                    val currentList = saveEvent.value.toMutableList()
                    val movedItem = currentList.removeAt(from)
                    currentList.add(to, movedItem)
                    // 2. 重新設定每個事件的 order 欄位
                    currentList.forEachIndexed { index, event ->
                        event.order = index
                    }
                    // 更新狀態以立即反映拖曳效果
                    saveEvent.value = currentList
                    Log.d("Greeting", "Reordered locally: from $from to $to, new order: ${currentList.map { it.order }}")

                    // 3. 在 IO 協程中更新資料庫，更新完後再重新讀取並更新 UI
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        database.eventDao().updateEvents(currentList)
                        // 重新讀取資料庫，確保順序正確
                        val updatedEvents = database.eventDao().getAllEvents()
                        Log.d("Greeting", "Database updated: ${updatedEvents.map { it.order }}")
                        launch(Dispatchers.Main) {
                            saveEvent.value = updatedEvents
                        }
                    }
                },
                navController = navController  // 傳遞 navController
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableEventList(
    events: List<Event>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    navController: NavController  // 添加 NavController 參數
) {
    // 建立拖曳排序狀態，使用新版 API (0.9.6)
    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            onMove(from.index, to.index)
        }
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current


    LazyColumn(
        state = reorderState.listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                )
            }
            .reorderable(reorderState)
            .detectReorderAfterLongPress(reorderState)
    ) {
        itemsIndexed(events) { index, event ->
            // 每個項目加入動畫效果
            EventCard(
                event = event,
                modifier = Modifier.animateItemPlacement(),
                navController = navController  // 傳遞 NavController
            )
        }
    }
}

@Composable
fun EventCard(
    event: Event, 
    modifier: Modifier = Modifier,
    navController: NavController  // 添加 NavController 參數
) {
    Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                // 導航到詳細頁面
                navController.navigate("detailScreen/${event.id}")
                Log.d("EventCard", "Navigating to detail for event: ${event.title}")
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (!event.imageUri.isNullOrEmpty()) {
                // 轉換檔案路徑成 File 與 Uri
                val file = File(event.imageUri)
                val fileUri = Uri.fromFile(file)
                val painter = rememberAsyncImagePainter(
                    model = fileUri,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background)
                )
                Log.d("EventCard", "File URI: $fileUri")
                Image(
                    painter = painter,
                    contentDescription = "卡片背景圖片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center) // 將 Column 置中在 Box 上
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ){
                // 修改：若 event.category 為 null 則預設 "D-DAY"
                val displayCategory = getDayDisplay(event.category, event.date)
                Text(
                    text = displayCategory,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 30.sp  // 新增字型大小為 24sp
                )
                Text(text = event.title, color = MaterialTheme.colorScheme.onSurface)
                Text(text = event.date, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// 新增輔助函式 getDayDisplay 的實作
fun getDayDisplay(category: String, dateStr: String): String {
    Log.d("getDayDisplay", "Called with category: $category, dateStr: $dateStr")
    // 修改這裡的 pattern，由 "yyyy/MM/dd" 改為 "yyyy/M/d" 以接受不補零的月份與日期
    val formatter = DateTimeFormatter.ofPattern("yyyy/M/d")
    val eventDate = try {
        LocalDate.parse(dateStr, formatter)
    } catch (ex: Exception) {
        Log.d("getDayDisplay", "Parsing failed for date: $dateStr, returning original")
        return dateStr
    }
    val today = LocalDate.now()
    Log.d("getDayDisplay", "Today: $today, Event Date: $eventDate")
    
    val result = when (category) {
        "D-DAY" -> {
            val diff = today.toEpochDay()-eventDate.toEpochDay()
            "D$diff 天"
        }
        "紀念日" -> {
            val diff = today.toEpochDay() - eventDate.toEpochDay()+1
            "D+$diff 天"
        }
        "每年" -> {
            var occurrence = eventDate.withYear(today.year)
            if (occurrence.isBefore(today)) {
                occurrence = occurrence.plusYears(1)
            }
            val diff =  today.toEpochDay()-occurrence.toEpochDay()
            "D$diff 天"
        }
        "每月" -> {
            var occurrence = eventDate.withYear(today.year).withMonth(today.monthValue)
            if (!occurrence.isAfter(today)) {
                occurrence = occurrence.plusMonths(1)
            }
            val diff = today.toEpochDay()-occurrence.toEpochDay()
            "D$diff 天"
        }
        else -> dateStr
    }
    Log.d("getDayDisplay", "Result: $result")
    return result
}

@Preview(showBackground = true)

@Composable
fun GreetingPreview() {
    MyCountDaysTheme {
        // 直接建立假資料狀態
        val dummyEvents = getDummyEvents()
        val saveEvent = remember { mutableStateOf(dummyEvents) }
        // 使用 rememberNavController() 傳入 navController
        Greeting(navController = rememberNavController(), saveEvent = saveEvent)
    }
}

//假資料
fun getDummyEvents(): List<Event> {
    return listOf(
        Event(
            id = 1,
            title = "生日派對",
            date = "2023/04/01",
            imageUri = "/data/user/0/com.example.mycountdays/files/dummy1.jpg",
            showNotification = true,
            notify100Days = false,
            notify1Year = false,
            category = "紀念日"
        ),
        Event(
            id = 2,
            title = "畢業典禮",
            date = "2023/04/10",
            imageUri = "", // 空字串表示沒有圖片
            showNotification = false,
            notify100Days = true,
            notify1Year = false,
            category = "D-DAY"
        ),
        Event(
            id = 3,
            title = "旅行計畫",
            date = "2023/05/01",
            imageUri = "/data/user/0/com.example.mycountdays/files/dummy2.jpg",
            showNotification = true,
            notify100Days = true,
            notify1Year = true,
            category = "D-DAY"
        )
    )
}
