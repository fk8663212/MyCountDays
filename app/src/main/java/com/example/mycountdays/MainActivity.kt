package com.example.mycountdays

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.mycountdays.screen.AddEventScreen
import com.example.mycountdays.ui.theme.MyCountDaysTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyCountDaysTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home"){
                    composable("addEventScreen"){
                        AddEventScreen(navController)
                    }
                    composable("home"){
                        Greeting(navController)
                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(navController : NavController) {
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
                items(10){
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
                                Text(text = "事件名稱")
                                Text(text = "日期")
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
        Greeting(navController = rememberNavController())
    }
}