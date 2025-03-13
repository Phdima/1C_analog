package com.example.a1cfucker.Project.domain.repository

import com.example.a1cfucker.Project.domain.model.BarcodeResult
import com.google.mlkit.vision.common.InputImage

interface BarcodeScannerRepository {
    suspend fun scan(image: InputImage): BarcodeResult
}