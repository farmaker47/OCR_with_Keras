package com.soloupis.sample.ocr_keras.fragments.ocr

import android.R.attr.bitmap
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.TransformToGrayscale
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


@SuppressWarnings("GoodTime")
class OcrModelExecutor(
    context: Context,
    private var useGPU: Boolean = false
) {

    private var numberThreads = 7
    private var fullExecutionTime = 0L
    private val interpreterPredict: Interpreter

    init {

        // Interpreter
        interpreterPredict = getInterpreter(context, OCR_MODEL, false)

    }

    companion object {
        private const val TAG = "OcrMExec"
        private const val CONTENT_IMAGE_WIDTH = 200
        private const val CONTENT_IMAGE_HEIGHT = 31

        private const val OCR_MODEL = "ocr_dr.tflite"
    }

    // Function for Interpreter
    fun executeOcrWithInterpreter(
        contentImage: Bitmap,
        context: Context
    ): LongArray {
        try {
            Log.i(TAG, "running models")

            fullExecutionTime = SystemClock.uptimeMillis()

            val arrayOutputs = Array(1) { LongArray(48) { 0 } }

            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(31, 200, ResizeOp.ResizeMethod.BILINEAR))
                .add(TransformToGrayscale())
                .add(NormalizeOp(0.0F, 255.0F))
                .build()

            var tImage = TensorImage(DataType.FLOAT32)

            // Preprocess the image
            tImage.load(contentImage)
            tImage = imageProcessor.process(tImage)

            interpreterPredict.run(
                tImage.buffer, arrayOutputs
            )

            Log.i(TAG, "after running")

            fullExecutionTime = SystemClock.uptimeMillis() - fullExecutionTime

            Log.i(TAG, "Time to run everything: $fullExecutionTime")

            return arrayOutputs[0]

        } catch (e: Exception) {

            val exceptionLog = "something went wrong: ${e.message}"
            Log.e("EXECUTOR", exceptionLog)

            return longArrayOf()
        }
    }

    private fun getByteBufferNormalized(bitmapIn: Bitmap): ByteBuffer {
        val bitmap = Bitmap.createScaledBitmap(
            bitmapIn,
            CONTENT_IMAGE_WIDTH,
            CONTENT_IMAGE_HEIGHT,
            true
        )
        val width = bitmap.width
        val height = bitmap.height
        // Below 4 is for floats and 2nd one (1) for grayscale
        val mImgData: ByteBuffer = ByteBuffer.allocateDirect(1 * width * height * 1 * 4)
        mImgData.order(ByteOrder.nativeOrder())
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (pixel in pixels) {
            mImgData.putFloat(Color.blue(pixel).toFloat() / 255.0f)
        }
        return mImgData
    }

    @Throws(IOException::class)
    private fun getInterpreter(
        context: Context,
        modelName: String,
        useGpu: Boolean = false
    ): Interpreter {
        val tfliteOptions = Interpreter.Options()

        tfliteOptions.setNumThreads(numberThreads)

        //tfliteOptions.setUseNNAPI(true)     //846ms
        //tfliteOptions.setUseXNNPACK(true) //     Caused by: java.lang.IllegalArgumentException: Internal error: Failed to apply XNNPACK delegate:
        //     Attempting to use a delegate that only supports static-sized tensors with a graph that has dynamic-sized tensors.

        return Interpreter(loadModelFile(context, modelName), tfliteOptions)
    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelFile: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val retFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        fileDescriptor.close()
        return retFile
    }

    fun androidGrayScale(bmpOriginal: Bitmap): Bitmap {
        val height: Int = bmpOriginal.height
        val width: Int = bmpOriginal.width
        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpGrayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val colorMatrixFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorMatrixFilter
        canvas.drawBitmap(bmpOriginal, 0f, 0f, paint)
        return bmpGrayscale
    }


    fun close() {
        interpreterPredict.close()
    }
}
