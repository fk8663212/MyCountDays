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
import android.net.Uri
import android.widget.DatePicker
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
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.mycountdays.data.AppDatabase
import com.example.mycountdays.data.Event
import com.example.mycountdays.data.EventApplication
import com.example.mycountdays.data.EventViewModel
import com.example.mycountdays.data.InventoryViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext



//資料庫

private lateinit var viewModel: EventViewModel
//private lateinit var database: AppDatabase



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(navController: NavController) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    var text by remember { mutableStateOf("") }
    var selectDate by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }


    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    var showNotification by remember { mutableStateOf(false) }
    var notify100Days by remember { mutableStateOf(true) }
    var notify1Year by remember { mutableStateOf(true) }




    //日期選擇器
    val datePickerDialog  = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            selectDate = "$year/${month + 1}/$dayOfMonth"
        },
        year,month,day
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri: Uri? ->
        imageUri = uri
    }

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
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 10.dp)
//                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text("類型", )
//                        Text("D-DAY", )
//                    }
//
//                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clickable(onClick = {
                            imagePickerLauncher.launch(PickVisualMediaRequest(ImageOnly))})
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
                        //新增置資料庫
                        val event = Event(
                            title = text,
                            date = selectDate,
                            imageUri = imageUri?.toString(),
                            showNotification = showNotification,
                            notify100Days = notify100Days,
                            notify1Year = notify1Year,
                            category = ""
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            database.eventDao().insertEvent(event)
                            database.eventDao().getAllEvents()

                        }

                        navController.navigate("home")

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





@Preview
@Composable
fun GreetingPreview() {
    MyCountDaysTheme {
        AddEventScreen(navController = rememberNavController())
    }
}