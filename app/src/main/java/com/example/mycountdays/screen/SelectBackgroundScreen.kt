package com.example.mycountdays.screen

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
    
    // 保存發起頁面的路由，用於控制裁剪完成後的返回目標
    val sourceRoute = remember {
        navController.previousBackStackEntry?.destination?.route ?: "unknown_route" 
    }
    Log.d(tag, "發起頁面路由: $sourceRoute")
    
    // 嘗試多種方式獲取圖片URI，首先從 savedStateHandle 獲取
    val imageUriString = navController.currentBackStackEntry?.savedStateHandle?.get<String>("imageUri")
        ?: navController.previousBackStackEntry?.arguments?.getString("imageUri")
        ?: navController.currentBackStackEntry?.arguments?.getString("imageUri")
    
    Log.d(tag, "接收到的圖片URI: $imageUriString")
    val imageUri = imageUriString?.let { Uri.parse(it) }
    
    // 圖片裁剪結果
    var croppedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // 裁剪選項
    val cropOptions = CropImageOptions(
        guidelines = CropImageView.Guidelines.ON,
        aspectRatioX = 1,
        aspectRatioY = 1,
        fixAspectRatio = true
    )
    
    // 裁剪圖片啟動器
    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // 獲取裁剪後的圖片URI
            croppedImageUri = result.uriContent
            Log.d(tag, "裁剪成功: $croppedImageUri")
            
            // 裁剪成功後，傳遞裁剪後的URI並返回
            croppedImageUri?.let { uri ->
                // 將裁剪後的 URI 設置到前一頁的 savedStateHandle
                navController.previousBackStackEntry?.savedStateHandle?.set("croppedImageUri", uri.toString())
                Log.d(tag, "保存裁剪URI到前一頁並返回: ${uri}")
                
                // 根據發起頁面的路由決定如何返回
                if (sourceRoute != "unknown_route") {
                    // 使用明確的返回導航，確保返回到正確的頁面
                    try {
                        // 嘗試直接返回到發起頁面
                        navController.popBackStack(sourceRoute, false)
                        Log.d(tag, "已返回到發起頁面: $sourceRoute")
                    } catch (e: Exception) {
                        // 如果上面的方法失敗，嘗試簡單的返回
                        Log.e(tag, "返回指定頁面失敗: ${e.message}, 嘗試簡單返回")
                        navController.popBackStack()
                    }
                } else {
                    // 如果無法確定發起頁面，使用簡單的返回
                    navController.popBackStack()
                }
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