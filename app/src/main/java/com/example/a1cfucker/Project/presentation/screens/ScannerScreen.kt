package com.example.a1cfucker.Project.presentation.screens

import android.graphics.ImageFormat
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.a1cfucker.Project.domain.model.BarcodeResult
import com.example.a1cfucker.Project.presentation.viewmodels.ScannerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val lifecycle = LocalLifecycleOwner.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    var lastScanTime by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                val cameraProvider = withContext(Dispatchers.Main) {
                    cameraProviderFuture.get()
                }
                cameraProvider.unbindAll()
                try {
                    if (!analyzerExecutor.isShutdown) {
                        analyzerExecutor.shutdownNow()
                        analyzerExecutor.awaitTermination(1, TimeUnit.SECONDS)
                    }
                } catch (e: InterruptedException) {
                    Log.e("Executor", "Error shutting down", e)
                }
            }

        }
    }

    when {
        cameraPermissionState.status.isGranted -> {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    coroutineScope.launch {
                        val cameraProvider = withContext(Dispatchers.Main) {
                            cameraProviderFuture.get()
                        }
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setResolutionSelector(
                                ResolutionSelector.Builder()
                                    .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                                    .build()
                            )
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(analyzerExecutor) { imageProxy ->
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastScanTime > 500) {
                                        processImage(imageProxy) { image, proxy ->
                                            viewModel.processImage(image, proxy)
                                        }
                                        lastScanTime = currentTime
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycle,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("Camera", "Ошибка камеры", e)
                        }
                    }
                }
            )
        }

        else -> {
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }
    when(val state = viewModel.barcodeState.value) {
        is BarcodeResult.Success -> {
            Log.d("UIscan", "Scanned: ${state.value}")
            Text("Scanned: ${state.value}")
        }
        is BarcodeResult.Error -> {
            Log.e("UIscan", "Error: ${state.message}")
            Text("Error: ${state.message}")
        }
        BarcodeResult.Loading -> {
            Log.d("UIscan", "Scanning...")
            CircularProgressIndicator()
        }
    }

}

private fun processImage(
    imageProxy: ImageProxy,
    onImageProcess: (InputImage, ImageProxy) -> Unit
) {
    try {
        val mediaImage = imageProxy.image ?: return
        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)
        onImageProcess(image, imageProxy)
    } catch (e: Exception) {
        Log.e("MLKit", "Image processing error", e)
        imageProxy.close()
    }
}