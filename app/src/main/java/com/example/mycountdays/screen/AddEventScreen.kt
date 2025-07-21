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
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
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

    // 使用 rememberSaveable 代替 remember 來保存狀態
    var text by rememberSaveable { mutableStateOf("") }
    var selectDate by rememberSaveable { mutableStateOf("") }
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    // 使用 rememberSaveable 保存通知設置
    var showNotification by rememberSaveable { mutableStateOf(false) }
    var notify100Days by rememberSaveable { mutableStateOf(true) }
    var notify1Year by rememberSaveable { mutableStateOf(true) }
    var category by rememberSaveable { mutableStateOf("") }


    // 圖片裁剪完成後的處理
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle?.get<String>("croppedImageUri")?.let { uriString ->
            Log.d(tag, "收到裁剪後的圖片 URI: $uriString")
            imageUri = Uri.parse(uriString)
            // 處理完後清除 savedStateHandle 中的數據
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("croppedImageUri")
        }
    }

    // 修改 LaunchedEffect 依賴項，使其能夠在每次返回頁面時都檢查裁剪結果
    // 使用 navController.currentBackStackEntry 作為依賴，確保頁面返回時重新檢查
    LaunchedEffect(navController.currentBackStackEntry) {
        Log.d(tag, "檢查裁剪結果...")
        
        // 嘗試從當前和上一個返回堆棧條目中獲取裁剪結果
        val croppedUriString = navController.currentBackStackEntry?.savedStateHandle?.get<String>("croppedImageUri")
            ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("croppedImageUri")
            
        Log.d(tag, "裁剪後的URI: $croppedUriString")
        
        croppedUriString?.let { uriString ->
            Log.d(tag, "收到裁剪後的圖片 URI: $uriString")
            imageUri = Uri.parse(uriString)
            
            // 從兩個可能的位置清除數據
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("croppedImageUri")
            navController.previousBackStackEntry?.savedStateHandle?.remove<String>("croppedImageUri")
        }
    }

    // 修改圖片選擇器，在選擇完圖片後導航到背景選擇頁面
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri: Uri? ->
        // 如果成功選擇圖片，導航到背景選擇頁面進行裁剪
        if (uri != null) {
            Log.d(tag, "選擇圖片 URI: $uri，導航到裁剪畫面")
            // 將 URI 保存到 savedStateHandle 以便傳遞給裁剪頁面
            navController.currentBackStackEntry?.savedStateHandle?.set("imageUri", uri.toString())
            navController.navigate("SelectBackgroundScreen")
        }
    }
    
    //日期選擇器
    val datePickerDialog  = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            selectDate = "$year/${month + 1}/$dayOfMonth"
        },
        year,month,day
    )

Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
        CenterAlignedTopAppBar(
            title = {
                Text(text = "Add Event")
            },
            navigationIcon = {

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
                        } ?: ""


                        //新增置資料庫
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

                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("建立")
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