package com.example.a1cfucker.Project.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.a1cfucker.Project.LocalNavController
import com.example.a1cfucker.Project.theme.*

@Composable
fun MainScreen() {
    val navController = LocalNavController.current


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Green1)
    ){
        Button(
            modifier = Modifier.align(Alignment.Center),
            onClick = {navController.navigate("ScannerScreen")},
            colors = ButtonDefaults.buttonColors(Green3)
        ) { }
    }
}