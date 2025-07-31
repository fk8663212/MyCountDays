package com.example.mycountdays.screen

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectBackgroundScreen(navController: NavController) {
    val context = LocalContext.current
    val tag = "SelectBackgroundScreen"
    
    // 從多個來源嘗試獲取圖片URI
    val imageUriString = navController.currentBackStackEntry?.savedStateHandle?.get<String>("imageUri")
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("imageUri")
        ?: try { 
            // 從應用層級嘗試獲取
            (context.applicationContext as? com.example.mycountdays.MyApplication)?.tempImageUri
        } catch (e: Exception) { null }
        ?: try {
            // 從SharedPreferences嘗試獲取
            context.getSharedPreferences("image_crop_temp", Context.MODE_PRIVATE)
                .getString("last_image_uri", null)
        } catch (e: Exception) { null }
    
    Log.d(tag, "接收到的圖片URI: $imageUriString")
    val imageUri = imageUriString?.let { Uri.parse(it) }
    
    // 圖片裁剪結果
    var croppedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // 修改裁剪選項，允許自由調整比例
    val cropOptions = CropImageOptions(
        guidelines = CropImageView.Guidelines.ON,
        // 不強制固定比例，允許使用者自由調整
        fixAspectRatio = false,
        // 提供初始比例建議，但不強制
        aspectRatioX = 1,
        aspectRatioY = 1,
        // 啟用自由調整比例功能
        showCropOverlay = true,
        // 允許調整裁剪框大小
        showProgressBar = true
        // 移除不存在的參數 cropperWindowPadding
    )
    
    // 裁剪圖片啟動器
    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // 獲取裁剪後的圖片URI
            croppedImageUri = result.uriContent
            Log.d(tag, "裁剪成功: $croppedImageUri")
            
            // 裁剪成功後，將結果保存到多個位置確保能被接收到
            croppedImageUri?.let { uri ->
                // 將裁剪結果保存到多個位置
                navController.previousBackStackEntry?.savedStateHandle?.set("croppedImageUri", uri.toString())
                navController.currentBackStackEntry?.savedStateHandle?.set("croppedImageUri", uri.toString())
                
                try {
                    // 保存到應用層級
                    val application = context.applicationContext
                    if (application is com.example.mycountdays.MyApplication) {
                        application.croppedImageUri = uri.toString()
                        Log.d(tag, "已將裁剪URI保存到應用程序級別: $uri")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "保存裁剪URI到應用層級時出錯: ${e.message}")
                }
                
                try {
                    // 保存到SharedPreferences
                    context.getSharedPreferences("image_crop_temp", Context.MODE_PRIVATE)
                        .edit()
                        .putString("last_cropped_uri", uri.toString())
                        .apply()
                    Log.d(tag, "已將裁剪URI保存到SharedPreferences")
                } catch (e: Exception) {
                    Log.e(tag, "保存裁剪URI到SharedPreferences時出錯: ${e.message}")
                }
                
                // 返回上一頁
                Log.d(tag, "準備返回上一頁")
                navController.popBackStack()
            }
        } else {
            // 裁剪失敗
            val error = result.error
            Log.e(tag, "裁剪失敗: ${error?.localizedMessage}")
            // 通知用戶裁剪失敗
            navController.popBackStack()
        }
    }
    
    // 進入頁面自動啟動裁剪
    LaunchedEffect(imageUri) {
        Log.d(tag, "準備啟動裁剪，URI: $imageUri")
        imageUri?.let {
            val cropOptions = CropImageContractOptions(it, cropOptions)
            cropImageLauncher.launch(cropOptions)
        } ?: run {
            // 如果沒有圖片URI，顯示錯誤訊息並在短暫延遲後返回
            Log.e(tag, "未接收到有效的圖片URI，無法進行裁剪")
            // 延遲一秒後返回，讓用戶有時間看到錯誤訊息
            kotlinx.coroutines.delay(1000)
            navController.popBackStack()
        }
    }
    
    // 顯示載入中的UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("正在準備圖片裁剪...")
            
            // 如果沒有圖片URI，顯示錯誤訊息
            if (imageUri == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "無法獲取圖片URI",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}