package com.example.mycountdays.screen

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.mycountdays.R
import com.example.mycountdays.data.Event
import com.example.mycountdays.getDummyEvents
import com.example.mycountdays.ui.theme.MyCountDaysTheme
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.mycountdays.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(event: Event, navController: NavController) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    var showMenu by remember { mutableStateOf(false) }
    
    // 計算已經過了多少天
    val formatter = DateTimeFormatter.ofPattern("yyyy/M/d")
    val startDate = LocalDate.parse(event.date, formatter)
    val today = LocalDate.now()
    val daysPassed = ChronoUnit.DAYS.between(startDate, today).toInt() + 1

    // 背景圖片處理
    val backgroundPainter = if (!event.imageUri.isNullOrEmpty()) {
        val file = File(event.imageUri)
        if (file.exists()) {
            val fileUri = Uri.fromFile(file)
            rememberAsyncImagePainter(model = fileUri)
        } else {
            painterResource(id = R.drawable.ic_launcher_background)
        }
    } else {
        painterResource(id = R.drawable.ic_launcher_background)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景圖片
        Image(
            painter = backgroundPainter,
            contentDescription = "事件背景圖片",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 漸層遮罩效果
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(event.title, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // 新增三點選單
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "更多選項",
                                tint = Color.White
                            )
                        }
                        
                        // 下拉選單
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // 修改選項
                            DropdownMenuItem(
                                text = { Text("修改") },
                                onClick = { 
                                    showMenu = false
                                    // 導航到修改頁面
                                    navController.navigate("addEventScreen?option=${event.category}") {
                                        // 將事件數據傳遞到下一個畫面
                                        navController.currentBackStackEntry?.savedStateHandle?.set("editEvent", event)
                                    }
                                }
                            )
                            
                            // 刪除選項
                            DropdownMenuItem(
                                text = { Text("刪除") },
                                onClick = { 
                                    showMenu = false
                                    // 刪除事件
                                    CoroutineScope(Dispatchers.IO).launch {
                                        database.eventDao().delete(event)
                                        
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "已刪除事件", Toast.LENGTH_SHORT).show()
                                            // 返回首頁
                                            navController.navigate("home") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            // 使用 LazyColumn 使內容可滾動
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 天數顯示部分 - 在中央位置
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp) // 給中央部分一個固定高度
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "已經過了",
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            
                            Text(
                                text = "$daysPassed",
                                fontSize = 60.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Text(
                                text = "天",
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                
                // 空白填充部分 - 確保紀念日計算在底部
                item {
                    Text(text = "起始日期: ${event.date}",
                            fontSize = 16.sp,
                            color = Color.White)
                }
                
                // 紀念日計算部分 - 在底部
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            when (event.category) {
                                "紀念日" -> MilestoneContentWithoutHeader(event.date)
                                "D-DAY" -> DdayContent(event.date)
                                "每年" -> YearlyContent(event.date)
                                "每月" -> MonthlyContent(event.date)
                                else -> MilestoneContentWithoutHeader(event.date)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 新增一個沒有標題和已過天數的 MilestoneContent 版本
@Composable
fun MilestoneContentWithoutHeader(startDateStr: String) {
    val formatter = DateTimeFormatter.ofPattern("yyyy/M/d")
    val startDate = LocalDate.parse(startDateStr, formatter)
    val today = LocalDate.now()
    
    Column {
        Text(
            text = "紀念日計算",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 計算每100天的紀念日
        val milestones = generateSequence(100) { it + 100 }
            .take(10)
            .map { days -> 
                val milestoneDate = startDate.plusDays(days.toLong() - 1)
                val daysRemaining = ChronoUnit.DAYS.between(today, milestoneDate).toInt()
                Triple(days, milestoneDate, daysRemaining)
            }
            .filter { it.third >= 0 }
            .toList()
        
        milestones.forEach { (days, date, remaining) ->
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "$days 天紀念日", color = Color.White)
                    Text(text = dateStr, color = Color.White)
                }
                Text(text = "還有 $remaining 天", color = Color.White)
            }
            Divider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
        }
    }
}

@Composable
fun CategoryChip(category: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun MilestoneContent(startDateStr: String) {
    val formatter = DateTimeFormatter.ofPattern("yyyy/M/d")
    val startDate = LocalDate.parse(startDateStr, formatter)
    val today = LocalDate.now()
    val daysPassed = ChronoUnit.DAYS.between(startDate, today).toInt() + 1
    
    Column {
        Text(
            text = "紀念日計算",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "已經過了 $daysPassed 天",
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 計算每100天的紀念日
        val milestones = generateSequence(100) { it + 100 }
            .take(10)
            .map { days -> 
                val milestoneDate = startDate.plusDays(days.toLong() - 1)
                val daysRemaining = ChronoUnit.DAYS.between(today, milestoneDate).toInt()
                Triple(days, milestoneDate, daysRemaining)
            }
            .filter { it.third >= 0 }
            .toList()
        
        milestones.forEach { (days, date, remaining) ->
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "$days 天紀念日", color = Color.White)
                Text(text = dateStr, color = Color.White)
                Text(text = "還有 $remaining 天", color = Color.White)
            }
            Divider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
        }
    }
}

@Composable
fun DdayContent(targetDateStr: String) {
    val formatter = DateTimeFormatter.ofPattern("yyyy/M/d")
    val targetDate = LocalDate.parse(targetDateStr, formatter)
    val today = LocalDate.now()
    val daysDiff = ChronoUnit.DAYS.between(today, targetDate).toInt()
    
    Column {
        Text(
            text = "D-DAY 計算",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val statusText = when {
            daysDiff > 0 -> "還有 $daysDiff 天"
            daysDiff < 0 -> "已經過了 ${-daysDiff} 天"
            else -> "就是今天！"
        }
        
        Text(
            text = statusText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 顯示前後7天的日期
        val dateRange = (-7..7)
            .map { offset -> 
                val date = targetDate.plusDays(offset.toLong())
                val dayDiff = ChronoUnit.DAYS.between(today, date).toInt()
                Pair(date, dayDiff)
            }
        
        dateRange.forEach { (date, diff) ->
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
            val diffText = when {
                diff > 0 -> "D-$diff"
                diff < 0 -> "D+${-diff}"
                else -> "D-Day"
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = diffText, color = Color.White)
                Text(text = dateStr, color = Color.White)
            }
            Divider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
        }
    }
}

@Composable
fun YearlyContent(startDateStr: String) {
    val formatter = DateTimeFormatter.ofPattern("yyyy/M/d")
    val startDate = LocalDate.parse(startDateStr, formatter)
    val today = LocalDate.now()
    
    // 計算每年紀念日
    val yearlyDates = (0..10).map { yearOffset -> 
        val anniversaryDate = startDate.plusYears(yearOffset.toLong())
        val daysDiff = ChronoUnit.DAYS.between(today, anniversaryDate).toInt()
        Triple(yearOffset, anniversaryDate, daysDiff)
    }
    
    Column {
        Text(
            text = "每年紀念日",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        yearlyDates.forEach { (year, date, daysDiff) ->
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
            val yearText = if (year == 0) "起始日" else "第 $year 年"
            val statusText = when {
                daysDiff > 0 -> "還有 $daysDiff 天"
                daysDiff < 0 -> "已過 ${-daysDiff} 天"
                else -> "就是今天！"
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = yearText, color = Color.White)
                Text(text = dateStr, color = Color.White)
                Text(text = statusText, color = Color.White)
            }
            Divider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
        }
    }
}

@Composable
fun MonthlyContent(startDateStr: String) {
    val formatter = DateTimeFormatter.ofPattern("yyyy/M/d")
    val startDate = LocalDate.parse(startDateStr, formatter)
    val today = LocalDate.now()
    
    // 計算每月紀念日（接下來12個月）
    val monthlyDates = (0..12).map { monthOffset -> 
        val currentDate = LocalDate.now().plusMonths(monthOffset.toLong())
        val monthlyDate = LocalDate.of(
            currentDate.year,
            currentDate.month,
            startDate.dayOfMonth.coerceAtMost(currentDate.month.length(currentDate.isLeapYear))
        )
        val daysDiff = ChronoUnit.DAYS.between(today, monthlyDate).toInt()
        Pair(monthlyDate, daysDiff)
    }.filter { it.second >= 0 }
    
    Column {
        Text(
            text = "每月紀念日",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        monthlyDates.forEach { (date, daysDiff) ->
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
            val statusText = when {
                daysDiff > 0 -> "還有 $daysDiff 天"
                else -> "就是今天！"
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = dateStr, color = Color.White)
                Text(text = statusText, color = Color.White)
            }
            Divider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    MyCountDaysTheme {
        // 使用假資料預覽
        val dummyEvent = getDummyEvents()[0].copy(category = "紀念日")
        DetailScreen(event = dummyEvent, navController = rememberNavController())
    }
}
