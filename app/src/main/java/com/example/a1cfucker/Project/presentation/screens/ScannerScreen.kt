package com.example.a1cfucker.Project.presentation.screens

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
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
                ScanningOverlay()
        }

        else -> {
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = viewModel.barcodeState.value) {
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
}

@Composable
fun ScanningOverlay() {
    val scanAreaSize = 280.dp
    val borderColor = Color.Green
    val borderWidth = 2.dp
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scanAreaPx = with(density) { scanAreaSize.toPx() }
            val centerX = size.width / 2
            val centerY = size.height / 2

            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, centerY - scanAreaPx / 2)
            )

            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                topLeft = Offset(0f, centerY + scanAreaPx / 2),
                size = Size(size.width, size.height - (centerY + scanAreaPx / 2))
            )

            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                topLeft = Offset(0f, centerY - scanAreaPx / 2),
                size = Size(centerX - scanAreaPx / 2, scanAreaPx)
            )

            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                topLeft = Offset(centerX + scanAreaPx / 2, centerY - scanAreaPx / 2),
                size = Size(size.width - (centerX + scanAreaPx / 2), scanAreaPx)
            )
        }

        Box(
            modifier = Modifier
                .size(scanAreaSize)
                .align(Alignment.Center)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp)
                )
        )

        val infiniteTransition = rememberInfiniteTransition()
        val scanOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing)
            ), label = ""
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(scanAreaSize)
                .clip(RoundedCornerShape(8.dp))
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color = borderColor.copy(alpha = 0.7f),
                        start = Offset(0f, size.height * scanOffset),
                        end = Offset(size.width, size.height * scanOffset),
                        strokeWidth = 4.dp.toPx()
                    )
                }
        )
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
        Log.e("ERROR", "Processing failed: ${e.stackTraceToString()}")
        imageProxy.close()
    }
}