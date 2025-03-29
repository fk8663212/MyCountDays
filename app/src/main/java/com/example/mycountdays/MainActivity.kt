package com.example.mycountdays

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.mycountdays.screen.AddEventScreen
import com.example.mycountdays.ui.theme.MyCountDaysTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mycountdays.data.AppDatabase
import com.example.mycountdays.data.Event


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
                items(saveEvent.value.size){
                    Column {
                        Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            ) {
                                Text(text = saveEvent.value[it].title)
                                Text(text = saveEvent.value[it].date)
                            }
                        }
                    }
                }
            }
        }
    }
}




@Composable
fun EventItem(){
    LazyColumn( modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
        items(10) {
            Column {
                Text(text = "事件名稱")
                Text(text = "日期")
            }
            }
    }

}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyCountDaysTheme {
        //Greeting(navController = rememberNavController())
    }
}