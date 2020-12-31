package com.soloupis.sample.ocr_keras.di

import com.soloupis.sample.ocr_keras.fragments.ocr.OcrViewModel
import com.soloupis.sample.ocr_keras.fragments.ocr.OcrModelExecutor
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val segmentationAndStyleTransferModule = module {

    factory { OcrModelExecutor(get(), false) }

    viewModel {
        OcrViewModel(get())
    }
}