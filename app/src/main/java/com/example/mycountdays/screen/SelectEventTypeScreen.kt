package com.example.mycountdays.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.mycountdays.Greeting
import com.example.mycountdays.getDummyEvents
import com.example.mycountdays.ui.theme.MyCountDaysTheme

@Composable
fun SelectEventTypeScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "選擇新增計日的種類", modifier = Modifier.padding(bottom = 24.dp))
        Button(
            onClick = { 
                navController.navigate("addEventScreen?option=D-DAY")
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("D-DAY")
        }
        Button(
            onClick = {
                navController.navigate("addEventScreen?option=紀念日")
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("紀念日")
        }
        Button(
            onClick = {
                navController.navigate("addEventScreen?option=每年")
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("每年")
        }
        Button(
            onClick = {
                navController.navigate("addEventScreen?option=月數與天數")
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("月數與天數")
        }
        Button(
            onClick = {
                navController.navigate("addEventScreen?option=年月日")
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("年月日")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectEventTypeScreenPreview() {
    MyCountDaysTheme {
        // 直接建立假資料狀態
        val dummyEvents = getDummyEvents()
        val saveEvent = remember { mutableStateOf(dummyEvents) }
        // 使用 rememberNavController() 傳入 navController
        SelectEventTypeScreen(navController = rememberNavController())
    }
}