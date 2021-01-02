package com.soloupis.sample.ocr_keras.fragments.ocr

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import org.koin.core.KoinComponent
import org.koin.core.get
import java.io.IOException

class OcrViewModel(application: Application) :
    AndroidViewModel(application),
    KoinComponent {

    private lateinit var scaledMaskBitmap: Bitmap
    private lateinit var outputArray: LongArray
    var startTime: Long = 0L
    var startTimeMlKit: Long = 0L

    var inferenceTime = 0L

    var seekBarProgress: Float = 0F

    private var _currentList: ArrayList<String> = ArrayList()
    val currentList: ArrayList<String>
        get() = _currentList

    private val _inferenceDone = MutableLiveData<Boolean>()
    val inferenceDone: LiveData<Boolean>
        get() = _inferenceDone

    private val _inferenceTimeMlKit = MutableLiveData<Long>()
    val inferenceTimeMlKit: LiveData<Long>
        get() = _inferenceTimeMlKit

    private val _mlKitOcrText = MutableLiveData<String>()
    val mlKitOcrtext: LiveData<String>
        get() = _mlKitOcrText

    val ocrModelExecutor: OcrModelExecutor

    init {

        _currentList.addAll(application.assets.list("thumbnails")!!)

        ocrModelExecutor = get()

        _inferenceTimeMlKit.value = 0L

    }

    fun onClickPerformOcr(
        context: Context,
        imageFilePath: String
    ): Pair<LongArray, Long> {

        return performOcrWithImage(imageFilePath, context)
    }

    fun performOcrWithImage(thumbnailsImageName: String, context: Context): Pair<LongArray, Long> {
        try {
            // Initialization
            startTime = SystemClock.uptimeMillis()

            // Run inference
            val result = ocrModelExecutor.executeOcrWithInterpreter(
                getBitmapFromAsset(
                    context,
                    "thumbnails/$thumbnailsImageName"
                ), context
            )
            outputArray = result

            inferenceTime = SystemClock.uptimeMillis() - startTime

        } catch (e: IOException) {
            Log.e("Ocr", "Error: ", e)
        }

        return Pair(outputArray, inferenceTime)
    }

    fun performOcrWithMlKit(thumbnailsImageName: String, context: Context) {

        startTimeMlKit = System.currentTimeMillis()

        val scaledBitmap = Bitmap.createScaledBitmap(
            getBitmapFromAsset(
                context,
                "thumbnails/$thumbnailsImageName"
            ),
            200,
            32,
            true
        )

        // Use grayscale bitmap as Keras OCR
        val image = InputImage.fromBitmap(
            ocrModelExecutor.androidGrayScale(scaledBitmap), 0
        )

        val recognizer = TextRecognition.getClient()
        recognizer.process(image)
            .addOnSuccessListener { texts ->
                Log.i("ML_Kit", texts.text)

                _mlKitOcrText.value = texts.text

                _inferenceTimeMlKit.value = System.currentTimeMillis() - startTimeMlKit
            }
            .addOnFailureListener { e -> // Task failed with an exception
                e.printStackTrace()
            }


    }

    private fun getBitmapFromAsset(context: Context, path: String): Bitmap =
        context.assets.open(path).use { BitmapFactory.decodeStream(it) }


    override fun onCleared() {
        super.onCleared()
        ocrModelExecutor.close()
    }

}