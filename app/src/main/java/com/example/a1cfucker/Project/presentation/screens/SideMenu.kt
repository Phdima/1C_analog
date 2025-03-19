package com.example.a1cfucker.Project.presentation.screens

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.a1cfucker.Project.LocalNavController
import com.example.a1cfucker.Project.theme.*
import kotlinx.coroutines.launch

@Composable
fun SideMenu(
    onItemClick: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .background(color = Green2)
                    .fillMaxHeight()
                    .width(150.dp)
            ) {

                Text("Главная", modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        scope.launch { drawerState.close() }
                        onItemClick("MainScreen")
                    })
                Text("Сканер", modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        scope.launch { drawerState.close() }
                        onItemClick("ScannerScreen")
                    })
            }
        }
    ) {
        content()
        IconButton(onClick = { scope.launch { drawerState.open() } }
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Меню")
        }
    }
}




