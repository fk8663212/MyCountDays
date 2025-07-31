package com.example.mycountdays.screen

import android.icu.util.Calendar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.mycountdays.ui.theme.MyCountDaysTheme
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.mycountdays.data.AppDatabase
import com.example.mycountdays.data.Event
import com.example.mycountdays.data.EventViewModel
import com.example.mycountdays.data.InventoryViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import java.io.IOException
import java.io.File
import com.example.mycountdays.notification.NotificationHelper


//資料庫





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(navController: NavController, option: String?) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val tag = "AddEventScreen"

    // 獲取要編輯的事件（如果有）
    val editEvent = navController.previousBackStackEntry?.savedStateHandle?.get<Event>("editEvent")
    val isEditing = editEvent != null
    
    // 根據是否在編輯模式設定初始值
    var text by rememberSaveable { mutableStateOf(editEvent?.title ?: "") }
    var selectDate by rememberSaveable { mutableStateOf(editEvent?.date ?: "") }
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(editEvent?.imageUri?.let { Uri.fromFile(File(it)) }) }
    var showNotification by rememberSaveable { mutableStateOf(editEvent?.showNotification ?: false) }
    var notify100Days by rememberSaveable { mutableStateOf(editEvent?.notify100Days ?: true) }
    var notify1Year by rememberSaveable { mutableStateOf(editEvent?.notify1Year ?: true) }
    var category by rememberSaveable { mutableStateOf(editEvent?.category ?: "") }


    // 圖片裁剪完成後的處理
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle?.get<String>("croppedImageUri")?.let { uriString ->
            Log.d(tag, "收到裁剪後的圖片 URI: $uriString")
            imageUri = Uri.parse(uriString)
            // 處理完後清除 savedStateHandle 中的數據
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("croppedImageUri")
        }
    }

    // 移除舊的 LaunchedEffect 區塊，合併為單一效果處理器
    // 使用一個計數器來跟踪生命週期，確保每次頁面可見時都檢查裁剪結果
    val lifeCycleCount = remember { mutableStateOf(0) }
    
    // 使用 DisposableEffect 監聽頁面生命週期
    DisposableEffect(Unit) {
        lifeCycleCount.value++
        onDispose { }
    }
    
    // 統一的裁剪結果處理邏輯，依賴於生命週期計數和導航控制器
    LaunchedEffect(lifeCycleCount.value, navController.currentBackStackEntry) {
        Log.d(tag, "檢查裁剪結果... (生命週期計數: ${lifeCycleCount.value})")
        
        // 安全地獲取可用的 savedStateHandle
        val sources = mutableListOf<androidx.navigation.NavBackStackEntry?>()
        sources.add(navController.currentBackStackEntry)
        sources.add(navController.previousBackStackEntry)
        
        // 改用更安全的方式檢查，不直接訪問私有屬性
        try {
            // 只嘗試最常見的情況，如果出現異常則忽略
            if (navController.currentDestination?.route?.contains("SelectBackgroundScreen") == true) {
                Log.d(tag, "當前頁面是 SelectBackgroundScreen")
            }
        } catch (e: Exception) {
            Log.e(tag, "檢查導航堆棧時出錯: ${e.message}")
        }
        
        // 在可用的來源中查找裁剪結果
        var croppedUriString: String? = null
        for ((index, entry) in sources.withIndex()) {
            val savedStateHandle = entry?.savedStateHandle
            val uri = savedStateHandle?.get<String>("croppedImageUri")
            if (uri != null) {
                Log.d(tag, "在來源 $index 找到裁剪結果: $uri")
                croppedUriString = uri
                break
            }
        }
            
        Log.d(tag, "最終裁剪結果URI: $croppedUriString")
        
        croppedUriString?.let { uriString ->
            Log.d(tag, "應用裁剪後的圖片 URI: $uriString")
            imageUri = Uri.parse(uriString)
            
            // 從所有可能的位置清除數據
            sources.forEach { it?.savedStateHandle?.remove<String>("croppedImageUri") }
            Log.d(tag, "已清除裁剪URI資料")
        }
    }

    // 添加新的狀態標誌來跟踪是否從裁剪頁面返回
    var isReturningFromCrop by rememberSaveable { mutableStateOf(false) }
    
    // 檢測從裁剪頁面返回
    val previousDestinationRoute = remember { mutableStateOf<String?>(null) }
    
    // 監聽導航變化，檢測從裁剪頁面返回的情況
    LaunchedEffect(navController.currentDestination) {
        val currentRoute = navController.currentDestination?.route
        Log.d(tag, "當前路由: $currentRoute, 前一個路由: ${previousDestinationRoute.value}")
        
        // 如果前一個頁面是裁剪頁面，且現在回到了添加頁面，則標記為從裁剪頁面返回
        if (previousDestinationRoute.value == "SelectBackgroundScreen" && currentRoute?.contains("addEventScreen") == true) {
            Log.d(tag, "檢測到從裁剪頁面返回")
            isReturningFromCrop = true
        }
        
        previousDestinationRoute.value = currentRoute
    }
    
    // 當從裁剪頁面返回時，立即檢查結果
    LaunchedEffect(isReturningFromCrop) {
        if (isReturningFromCrop) {
            Log.d(tag, "從裁剪頁面返回，開始檢查結果")
            
            // 安全地獲取可用的 savedStateHandle
            val sources = mutableListOf<androidx.navigation.NavBackStackEntry?>()
            sources.add(navController.currentBackStackEntry)
            sources.add(navController.previousBackStackEntry)
            
            // 在可用的來源中查找裁剪結果
            var croppedUriString: String? = null
            for ((index, entry) in sources.withIndex()) {
                val savedStateHandle = entry?.savedStateHandle
                val uri = savedStateHandle?.get<String>("croppedImageUri")
                if (uri != null) {
                    Log.d(tag, "在來源 $index 找到裁剪結果: $uri")
                    croppedUriString = uri
                    break
                }
            }
                
            Log.d(tag, "最終裁剪結果URI: $croppedUriString")
            
            croppedUriString?.let { uriString ->
                Log.d(tag, "應用裁剪後的圖片 URI: $uriString")
                imageUri = Uri.parse(uriString)
                
                // 從所有可能的位置清除數據
                sources.forEach { it?.savedStateHandle?.remove<String>("croppedImageUri") }
                Log.d(tag, "已清除裁剪URI資料")
            }
            
            // 重置標誌
            isReturningFromCrop = false
        }
    }
    
    // 修改圖片選擇器，確保傳遞所有必要信息
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri: Uri? ->
        // 如果成功選擇圖片，導航到背景選擇頁面進行裁剪
        if (uri != null) {
            Log.d(tag, "選擇圖片 URI: $uri，導航到裁剪畫面")
            
            // 將所有關鍵信息保存到多個位置，增加被正確接收的機會
            navController.currentBackStackEntry?.savedStateHandle?.set("imageUri", uri.toString())
            navController.currentBackStackEntry?.savedStateHandle?.set("sourceScreen", "addEventScreen")
            
            // 在全局對象上也保存一份
            try {
                val application = context.applicationContext
                if (application is com.example.mycountdays.MyApplication) {
                    application.tempImageUri = uri.toString()
                    Log.d(tag, "已將圖片URI保存到應用程序級別: $uri")
                }
            } catch (e: Exception) {
                Log.e(tag, "保存URI到應用層級時出錯: ${e.message}")
            }
            
            // 在SharedPreferences中也臨時保存一份
            try {
                context.getSharedPreferences("image_crop_temp", Context.MODE_PRIVATE)
                    .edit()
                    .putString("last_image_uri", uri.toString())
                    .apply()
                Log.d(tag, "已將圖片URI保存到SharedPreferences")
            } catch (e: Exception) {
                Log.e(tag, "保存URI到SharedPreferences時出錯: ${e.message}")
            }
            
            // 導航到裁剪頁面
            navController.navigate("SelectBackgroundScreen")
        }
    }
    
    // 獲取當前日期
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    
    //日期選擇器
    val datePickerDialog  = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            selectDate = "$year/${month + 1}/$dayOfMonth"
        },
        year, month, day
    )

Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
        CenterAlignedTopAppBar(
            title = {
                Text(text = if (isEditing) "編輯事件" else "新增事件")
            },
            navigationIcon = {
                // 新增返回按鈕
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )
    }
    ) {
        paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                //置中
        ){
            Column(
                //至中
                modifier = Modifier
                    .padding(20.dp, vertical = 10.dp)
            ){
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)

                ) {

                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("請輸入標題") },
                        modifier = Modifier.fillMaxWidth()
                    )

                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clickable {
                            datePickerDialog.show()
                        }

                ) {
                    TextField(
                        value = selectDate,
                        onValueChange = {},
                        label = { Text("請選擇日期") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        trailingIcon = {
                            IconButton(onClick = {
                                //使用原生選擇日期
                                datePickerDialog.show()
                            }){
                                Icon(painter = painterResource(id = android.R.drawable.ic_menu_today), contentDescription = "選擇日期")
                            }
                        }
                    )


                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clickable(onClick = {
                            // 直接啟動系統圖片選擇器
                            imagePickerLauncher.launch(PickVisualMediaRequest(ImageOnly))
                        })
                ){
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("封面圖片")
                        if (imageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUri),
                                contentDescription = "選擇的封面圖片",
                                modifier = Modifier.size(50.dp),
                                contentScale = ContentScale.Crop)
                        }
                        else{
                            Image(
                                painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                                contentDescription = "封面圖片",
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clickable(onClick = {
                            navController.navigate("selectEventTypeScreen")
                        })
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("類型")
                        // 測試用方式取得 option (若傳入 null 則試著從 navController 讀取)
                        Log.d("AddEventScreen", "option: $option")
                        category = option ?: navController.currentBackStackEntry?.arguments?.getString("option") ?: ""
                        Text(text = category)
                    }

                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("在通知欄顯示活動")
                        Switch(checked = showNotification, onCheckedChange = {showNotification=it})
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("通知100天紀念日")
                        Switch(checked = notify100Days, onCheckedChange = {notify100Days=it})
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("通知1年紀念日")
                        Switch(checked = notify1Year, onCheckedChange = {notify1Year=it})
                    }
                }


                Button(
                    onClick = {
                        if (text.isEmpty()) {
                            Toast.makeText(context, "請輸入標題", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val imagePath = imageUri?.let { uri ->
                            saveImageToInternalStorage(context, uri)
                        } ?: editEvent?.imageUri ?: ""


                        //如果是編輯模式，保留原來的 id 並更新事件
                        if (isEditing) {
                            val updatedEvent = editEvent!!.copy(
                                title = text,
                                date = selectDate,
                                imageUri = imagePath,
                                showNotification = showNotification,
                                notify100Days = notify100Days,
                                notify1Year = notify1Year,
                                category = category
                            )
                            
                            CoroutineScope(Dispatchers.IO).launch {
                                database.eventDao().updateEvents(listOf(updatedEvent))
                                
                                // 處理通知
                                withContext(Dispatchers.Main) {
                                    val notificationHelper = NotificationHelper(context)
                                    if (showNotification) {
                                        notificationHelper.showPersistentNotification(updatedEvent)
                                    } else {
                                        notificationHelper.removePersistentNotification(updatedEvent.id)
                                    }
                                    
                                    // 清除編輯狀態
                                    navController.previousBackStackEntry?.savedStateHandle?.remove<Event>("editEvent")
                                    
                                    // 返回首頁
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            }
                        } else {
                            // 新增事件，原有邏輯
                            val event = Event(
                                title = text,
                                date = selectDate,
                                imageUri = imagePath,
                                showNotification = showNotification,
                                notify100Days = notify100Days,
                                notify1Year = notify1Year,
                                category = category
                            )
                            
                            CoroutineScope(Dispatchers.IO).launch {
                                database.eventDao().insertEvent(event)
                                
                                // 處理通知
                                if (showNotification) {
                                    withContext(Dispatchers.Main) {
                                        val notificationHelper = NotificationHelper(context)
                                        notificationHelper.showPersistentNotification(event)
                                    }
                                }
                                
                                database.eventDao().getAllEvents()
                            }

                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isEditing) "儲存" else "建立")
                }
            }


        }


    }
}

fun saveImageToInternalStorage(context: Context, imageUri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        if (inputStream == null) {
            Log.e("SaveImage", "Unable to open InputStream for URI: $imageUri")
            return null
        }
        // 產生檔案名稱，避免重複
        val fileName = "event_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        // 嘗試建立新檔案
        if (!file.exists()) {
            file.createNewFile()
        }
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Log.d("SaveImage", "File saved: ${file.absolutePath}, exists: ${file.exists()}")
        file.absolutePath
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}






@Preview
@Composable
fun GreetingPreview() {
    MyCountDaysTheme {
        AddEventScreen(navController = rememberNavController(), option = null)
    }
}