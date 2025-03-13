package com.example.a1cfucker.Project.di

import com.example.a1cfucker.Project.data.BarcodeScannerRepositoryImpl
import com.example.a1cfucker.Project.domain.repository.BarcodeScannerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BarCodeModule {
    @Provides
    fun provideBarcodeScanner(): BarcodeScannerRepository = BarcodeScannerRepositoryImpl()
}