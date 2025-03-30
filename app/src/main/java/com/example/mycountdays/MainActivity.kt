package com.example.mycountdays

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.mycountdays.screen.AddEventScreen
import com.example.mycountdays.ui.theme.MyCountDaysTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import coil.compose.rememberAsyncImagePainter
import com.example.mycountdays.data.AppDatabase
import com.example.mycountdays.data.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable



class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        database = AppDatabase.getDatabase(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            var saveEvent= remember { mutableStateOf<List<Event>>(emptyList()) }
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(lifecycleOwner) {
                // 立即讀取一次資料庫
                saveEvent.value = database.eventDao().getAllEvents()
                Log.d("MainActivity", "Initial load: ${saveEvent.value}")
            }


            MyCountDaysTheme {
                val navController = rememberNavController()

                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { backStackEntry ->
                        if (backStackEntry.destination.route == "home") {
                            // 這裡呼叫 suspend 函數 getAllEvents()，自動在協程中執行
                            saveEvent.value = database.eventDao().getAllEvents()
                            Log.d("MainActivity", "Reload on home: ${saveEvent.value}")
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "home"){
                    composable("addEventScreen"){
                        AddEventScreen(navController)
                    }
                    composable("home"){
                        Greeting(navController, saveEvent =saveEvent)
                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(navController: NavController, saveEvent: MutableState<List<Event>>) {
    Scaffold (
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "My Count Days")
                },
                navigationIcon = {

                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                //使用navigation-compose 切換 addEventScreen
                navController.navigate("addEventScreen")
                //切回後更新畫面


            }) {
                Icon(Icons.Default.Add, contentDescription = "新增事件")
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center
    ){
        paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ){
            LazyColumn(
                modifier = Modifier
                    .padding(20.dp, vertical = 10.dp)
            ) {
                items(saveEvent.value.size){ index ->
                    // 取得目前的事件
                    val event = saveEvent.value[index]
                    Column {
                        EventCard(event = event)
                    }
                }
            }
        }
    }
}

//@Composable
//fun ReorderableEventList(
//    events: List<Event>,
//    onMove: (from: Int, to: Int) -> Unit
//) {
//    // 使用庫提供的狀態
//    val state = rememberReorderableLazyListState(onMove = { from, to ->
//
//    })
//
//    LazyColumn(
//        modifier = Modifier
//            .reorderable(
//                state = reorderState,
//                onMove = { from, to -> onMove(from.index, to.index) }
//            )
//            .detectReorderAfterLongPress(reorderState)
//    ) {
//        itemsIndexed(events) { index, event ->
//            // 根據 reorderState.highlightedItemIndex 判斷是否正在拖曳
//            EventCard(event = event)
//        }
//    }
//}


@Composable
fun EventCard(event: Event) {
    Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (!event.imageUri.isNullOrEmpty()) {
                val file = File(event.imageUri)
                val fileUri = Uri.fromFile(file)
                val painter = rememberAsyncImagePainter(
                    model = fileUri,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background), // 請替換成您實際的資源
                )

                Log.d("Greeting", "File URI: $fileUri")
                Image(
                    painter = painter,
                    contentDescription = "卡片背景圖片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(text = event.title, color = MaterialTheme.colorScheme.onSurface)
                Text(text = event.date, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyCountDaysTheme {
        Greeting(navController = rememberNavController(), saveEvent = remember { mutableStateOf<List<Event>>(emptyList()) })
    }
}