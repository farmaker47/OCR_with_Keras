package com.soloupis.sample.ocr_keras.fragments.ocr

import android.app.Application
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.koin.core.KoinComponent
import org.koin.core.get
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter
import java.io.IOException

class OcrViewModel(application: Application) :
        AndroidViewModel(application),
        KoinComponent {

    private lateinit var imageSegmenter: ImageSegmenter
    private lateinit var scaledMaskBitmap: Bitmap
    private lateinit var outputArray: LongArray
    var startTime: Long = 0L
    var inferenceTime = 0L
    lateinit var scaledBitmapObject: Bitmap

    var seekBarProgress: Float = 0F

    private var _currentList: ArrayList<String> = ArrayList()
    val currentList: ArrayList<String>
        get() = _currentList

    private val _totalTimeInference = MutableLiveData<Int>()
    val totalTimeInference: LiveData<Int>
        get() = _totalTimeInference

    private val _styledBitmap = MutableLiveData<ModelExecutionResult>()
    val styledBitmap: LiveData<ModelExecutionResult>
        get() = _styledBitmap

    private val _inferenceDone = MutableLiveData<Boolean>()
    val inferenceDone: LiveData<Boolean>
        get() = _inferenceDone

    val ocrModelExecutor: OcrModelExecutor

    init {

        _currentList.addAll(application.assets.list("thumbnails")!!)

        ocrModelExecutor = get()

    }

    fun setTheSeekBarProgress(progress: Float) {
        seekBarProgress = progress
    }

    fun onClickPerformOcr(
            context: Context,
            styleFilePath: String
    ): Pair<LongArray, Long> {

        /*viewModelScope.launch(Dispatchers.Default) {
            inferenceExecute(contentBitmap, styleFilePath, context)
        }*/
        return performOcrWithImage(styleFilePath,context)
    }

    private fun inferenceExecute(
            contentBitmap: Bitmap,
            styleFilePath: String,
            context: Context
    ) {


        /*val result = ocrModelExecutor.executeWithMLBinding(contentBitmap, styleFilePath, context)

        _totalTimeInference.postValue(result.totalExecutionTime.toInt())
        _styledBitmap.postValue(result)
        _inferenceDone.postValue(true)*/
    }

    fun performOcr(bitmap: Bitmap, context: Context): Pair<LongArray, Long> {
        try {
            // Initialization
            startTime = SystemClock.uptimeMillis()

            // Run inference
            val result = ocrModelExecutor.executeOcrWithInterpreter(bitmap, context)
            outputArray = result

            inferenceTime = SystemClock.uptimeMillis() - startTime
        } catch (e: IOException) {
            Log.e("Ocr", "Error: ", e)
        }

        return Pair(outputArray, inferenceTime)
    }

    fun performOcrWithImage(thumbnailsImageName: String, context: Context): Pair<LongArray, Long> {
        try {
            // Initialization
            startTime = SystemClock.uptimeMillis()

            // Run inference
            val result = ocrModelExecutor.executeOcrWithInterpreter(getBitmapFromAsset(context,"thumbnails/$thumbnailsImageName"), context)
            outputArray = result

            inferenceTime = SystemClock.uptimeMillis() - startTime

        } catch (e: IOException) {
            Log.e("Ocr", "Error: ", e)
        }

        return Pair(outputArray, inferenceTime)
    }

    private fun getBitmapFromAsset(context: Context, path: String): Bitmap =
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }


    override fun onCleared() {
        super.onCleared()
        ocrModelExecutor.close()
    }

}