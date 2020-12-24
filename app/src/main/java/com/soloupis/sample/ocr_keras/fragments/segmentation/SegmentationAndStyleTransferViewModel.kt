package com.soloupis.sample.ocr_keras.fragments.segmentation

import android.app.Application
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.get
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter
import java.io.IOException

class SegmentationAndStyleTransferViewModel(application: Application) :
        AndroidViewModel(application),
        KoinComponent {

    private lateinit var imageSegmenter: ImageSegmenter
    private lateinit var scaledMaskBitmap: Bitmap
    private lateinit var outputArray: IntArray
    var startTime: Long = 0L
    var inferenceTime = 0L
    lateinit var scaledBitmapObject: Bitmap

    var stylename = String()
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

        stylename = "mona.JPG"

        _currentList.addAll(application.assets.list("thumbnails")!!)

        ocrModelExecutor = get()

    }

    fun setStyleName(string: String) {
        stylename = string
    }

    fun setTheSeekBarProgress(progress: Float) {
        seekBarProgress = progress
    }

    fun onApplyStyle(
            context: Context,
            contentBitmap: Bitmap,
            styleFilePath: String
    ) {

        /*viewModelScope.launch(Dispatchers.Default) {
            inferenceExecute(contentBitmap, styleFilePath, context)
        }*/
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

    fun performOcr(bitmap: Bitmap, context: Context): Pair<IntArray, Long> {
        try {
            // Initialization
            startTime = SystemClock.uptimeMillis()

            // Run inference
            val result = ocrModelExecutor.executeOcrWithMLBinding(bitmap, context)
            Log.e("RESULT", result.toString())

            inferenceTime = SystemClock.uptimeMillis() - startTime
        } catch (e: IOException) {
            Log.e("Ocr", "Error: ", e)
        }

        return Pair(outputArray, inferenceTime)
    }


    fun cropBitmapWithMask(original: Bitmap, mask: Bitmap?): Bitmap? {
        if (mask == null
        ) {
            return null
        }
        Log.i("ORIGINAL_WIDTH", original.width.toString())
        Log.i("ORIGINAL_HEIGHT", original.height.toString())
        Log.i("MASK_WIDTH", original.width.toString())
        Log.i("MASK_HEIGHT", original.height.toString())
        val w = original.width
        val h = original.height
        if (w <= 0 || h <= 0) {
            return null
        }
        val cropped: Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Log.i("CROPPED_WIDTH", cropped.width.toString())
        Log.i("CROPPED_HEIGHT", cropped.height.toString())
        val canvas = Canvas(cropped)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(original, 0f, 0f, null)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        paint.xfermode = null

        return cropped
    }

    fun cropBitmapWithMaskForStyle(original: Bitmap, mask: Bitmap?): Bitmap? {
        if (mask == null
        ) {
            return null
        }
        val w = original.width
        val h = original.height
        if (w <= 0 || h <= 0) {
            return null
        }

        val scaledBitmap = Bitmap.createScaledBitmap(
                mask,
                w,
                h, true
        )

        val cropped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cropped)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        canvas.drawBitmap(original, 0f, 0f, null)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)
        paint.xfermode = null
        return cropped
    }

    override fun onCleared() {
        super.onCleared()
        ocrModelExecutor.close()
    }

}