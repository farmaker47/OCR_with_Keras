package com.soloupis.sample.segmentationandstyletransfer.fragments.segmentation

import android.app.Application
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import org.koin.core.KoinComponent
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter
import org.tensorflow.lite.task.vision.segmenter.OutputType
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import java.io.IOException

class SegmentationAndStyleTransferViewModel(application: Application) : AndroidViewModel(application),
    KoinComponent {

    private lateinit var imageSegmenter: ImageSegmenter
    private lateinit var scaledMaskBitmap: Bitmap
    var startTime: Long = 0L
    var inferenceTime = 0L

    init {

    }

    fun cropPersonFromPhoto(bitmap: Bitmap): Pair<Bitmap?, Long> {
        try {
            // Initialization
            startTime = SystemClock.uptimeMillis()
            val options =
                ImageSegmenter.ImageSegmenterOptions.builder().setOutputType(OutputType.CATEGORY_MASK).build()
            imageSegmenter =
                ImageSegmenter.createFromFileAndOptions(
                    getApplication(),
                    "deeplabv3.tflite",
                    options
                )

            // Run inference
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val results: List<Segmentation> = imageSegmenter.segment(tensorImage)
            Log.i("LIST", results[0].toString())
            val result = results[0]
            val tensorMask = result.masks[0]
            Log.i("RESULT", result.coloredLabels.toString())
            val rawMask = tensorMask.tensorBuffer.intArray
            Log.i("NUMBER", rawMask.size.toString())
            Log.i("VALUES", rawMask.contentToString())

            val output = Bitmap.createBitmap(
                tensorMask.width,
                tensorMask.height,
                Bitmap.Config.ARGB_8888
            )
            for (y in 0 until tensorMask.height) {
                for (x in 0 until tensorMask.width) {
                    output.setPixel(
                        x,
                        y,
                        if (rawMask[y * tensorMask.width + x] == 0) Color.TRANSPARENT else Color.BLACK
                    )
                }
            }
            scaledMaskBitmap =
                Bitmap.createScaledBitmap(output, bitmap.getWidth(), bitmap.getHeight(), true)
            inferenceTime = SystemClock.uptimeMillis() - startTime
        } catch (e: IOException) {
            Log.e("ImageSegmenter", "Error: ", e)
        }

        return Pair(cropBitmapWithMask(bitmap, scaledMaskBitmap), inferenceTime)
    }


    private fun cropBitmapWithMask(original: Bitmap, mask: Bitmap?): Bitmap? {
        if (mask == null
        ) {
            return null
        }
        val w = original.width
        val h = original.height
        if (w <= 0 || h <= 0) {
            return null
        }
        val cropped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cropped)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(original, 0f, 0f, null)
        canvas.drawBitmap(mask, 0f, 0f, paint)
        paint.xfermode = null
        return cropped
    }


}