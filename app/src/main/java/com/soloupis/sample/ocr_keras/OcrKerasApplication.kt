package com.soloupis.sample.ocr_keras

import android.app.Application
import com.soloupis.sample.ocr_keras.di.ocrTransferModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OcrKerasApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            //androidContext(applicationContext)
            androidContext(this@OcrKerasApplication)
            modules(
                    ocrTransferModule
            )
        }

    }

}