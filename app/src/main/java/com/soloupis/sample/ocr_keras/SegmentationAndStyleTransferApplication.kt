package com.soloupis.sample.ocr_keras

import android.app.Application
import com.soloupis.sample.ocr_keras.di.segmentationAndStyleTransferModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SegmentationAndStyleTransferApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            //androidContext(applicationContext)
            androidContext(this@SegmentationAndStyleTransferApplication)
            modules(
                segmentationAndStyleTransferModule
            )
        }

    }

}