package com.example.a1cfucker.Project.domain.model

sealed class BarcodeResult {
    data class Success(val value: String) : BarcodeResult()
    data class Error(val message: String) : BarcodeResult()
    object Loading : BarcodeResult()
}