package com.example.a1cfucker.Project.data

import android.util.Log
import com.example.a1cfucker.Project.domain.model.BarcodeResult
import com.example.a1cfucker.Project.domain.repository.BarcodeScannerRepository
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await

class BarcodeScannerRepositoryImpl : BarcodeScannerRepository {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    override suspend fun scan(image: InputImage): BarcodeResult {
        return try {
            Log.d("MLKit", "Scanning image (${image.width}x${image.height}), format: ${image.format}")
            val result = scanner.process(image).await()
            Log.d("MLKit", "Scan result count: ${result.size}")
            result.firstOrNull()?.rawValue?.let {
                BarcodeResult.Success(it)
            } ?: BarcodeResult.Error("Barcode not found")
        } catch (e: Exception) {
            Log.e("MLKit", "Scan failed: ${e.javaClass.simpleName}", e)
            BarcodeResult.Error(e.message ?: "Unknown error")
        }
    }
}