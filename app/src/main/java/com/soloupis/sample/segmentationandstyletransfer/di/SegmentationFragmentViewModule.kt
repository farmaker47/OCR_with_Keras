package com.soloupis.sample.segmentationandstyletransfer.di

import com.soloupis.sample.segmentationandstyletransfer.fragments.segmentation.SegmentationAndStyleTransferViewModel
import com.soloupis.sample.segmentationandstyletransfer.fragments.segmentation.StyleTransferModelExecutor
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val segmentationAndStyleTransferModule = module {

    factory { StyleTransferModelExecutor(get(), false) }

    viewModel {
        SegmentationAndStyleTransferViewModel(get())
    }
}