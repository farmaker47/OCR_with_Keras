package com.soloupis.sample.ocr_keras.di

import com.soloupis.sample.ocr_keras.fragments.segmentation.SegmentationAndStyleTransferViewModel
import com.soloupis.sample.ocr_keras.fragments.segmentation.StyleTransferModelExecutor
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val segmentationAndStyleTransferModule = module {

    factory { StyleTransferModelExecutor(get(), false) }

    viewModel {
        SegmentationAndStyleTransferViewModel(get())
    }
}