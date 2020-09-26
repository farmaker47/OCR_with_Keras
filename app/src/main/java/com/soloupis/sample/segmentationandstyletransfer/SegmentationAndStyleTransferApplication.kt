package com.soloupis.sample.segmentationandstyletransfer

import android.app.Application
import com.soloupis.sample.segmentationandstyletransfer.di.segmentationAndStyleTransferModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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