package com.example.a1cfucker.Project.presentation.viewmodels

import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a1cfucker.Project.domain.model.BarcodeResult
import com.example.a1cfucker.Project.domain.repository.BarcodeScannerRepository
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scannerRepo: BarcodeScannerRepository
) : ViewModel() {
    private val _barcodeState = mutableStateOf<BarcodeResult>(BarcodeResult.Loading)
    val barcodeState: State<BarcodeResult> = _barcodeState

    fun processImage(image: InputImage, imageProxy: ImageProxy) {
        viewModelScope.launch {
            imageProxy.use {
                _barcodeState.value = scannerRepo.scan(image)
            }
        }
    }
}