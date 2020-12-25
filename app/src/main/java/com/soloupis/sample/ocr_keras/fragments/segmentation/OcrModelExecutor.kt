package com.soloupis.sample.ocr_keras.fragments.segmentation

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
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


data class ModelExecutionResult(
    val intArray: Bitmap,
    val preProcessTime: Long = 0L,
    val stylePredictTime: Long = 0L,
    val styleTransferTime: Long = 0L,
    val postProcessTime: Long = 0L,
    val totalExecutionTime: Long = 0L,
    val executionLog: String = "",
    val errorMessage: String = ""
)

@SuppressWarnings("GoodTime")
class OcrModelExecutor(
    context: Context,
    private var useGPU: Boolean = false
) {

    private var numberThreads = 2
    private var fullExecutionTime = 0L
    private var preProcessTime = 0L
    private var stylePredictTime = 0L
    private var styleTransferTime = 0L
    private var postProcessTime = 0L
    private val interpreterPredict: Interpreter

    init {

        // Interpreter
        interpreterPredict = getInterpreter(context, OCR_MODEL, false)

    }

    companion object {
        private const val TAG = "OcrMExec"
        private const val CONTENT_IMAGE_WIDTH = 31
        private const val CONTENT_IMAGE_HEIGHT = 200

        private const val OCR_MODEL = "ocr_dr.tflite"
    }

    /*// Function for ML Binding
    fun executeWithMLBinding(
            contentImagePath: Bitmap,
            styleImageName: String,
            context: Context
    ): ModelExecutionResult {
        try {
            Log.i(TAG, "running models")

            fullExecutionTime = SystemClock.uptimeMillis()

            preProcessTime = SystemClock.uptimeMillis()
            // Creates inputs for reference.
            val styleBitmap = ImageUtils.loadBitmapFromResources(context, "thumbnails/$styleImageName")
            val styleImage = TensorImage.fromBitmap(styleBitmap)
            val contentImage = TensorImage.fromBitmap(contentImagePath)
            preProcessTime = SystemClock.uptimeMillis() - preProcessTime

            stylePredictTime = SystemClock.uptimeMillis()
            // The results of this inference could be reused given the style does not change
            // That would be a good practice in case this was applied to a video stream.
            // Runs model inference and gets result.
            val outputsPredict = modelMlBindingPredict.process(styleImage)
            val styleBottleneckPredict = outputsPredict.styleBottleneckAsTensorBuffer
            stylePredictTime = SystemClock.uptimeMillis() - stylePredictTime
            Log.d(TAG, "Style Predict Time to run: $stylePredictTime")

            styleTransferTime = SystemClock.uptimeMillis()
            // Runs model inference and gets result.
            val outputs = modelMlBindingTransfer.process(contentImage, styleBottleneckPredict)
            styleTransferTime = SystemClock.uptimeMillis() - styleTransferTime
            Log.d(TAG, "Style apply Time to run: $styleTransferTime")

            postProcessTime = SystemClock.uptimeMillis()
            val styledImage = outputs.styledImageAsTensorImage
            val styledImageBitmap = styledImage.bitmap
            postProcessTime = SystemClock.uptimeMillis() - postProcessTime

            fullExecutionTime = SystemClock.uptimeMillis() - fullExecutionTime
            Log.d(TAG, "Time to run everything: $fullExecutionTime")

            return ModelExecutionResult(
                    styledImageBitmap,
                    preProcessTime,
                    stylePredictTime,
                    styleTransferTime,
                    postProcessTime,
                    fullExecutionTime,
                    formatExecutionLog()
            )
        } catch (e: Exception) {
            val exceptionLog = "something went wrong: ${e.message}"
            Log.d(TAG, exceptionLog)

            val emptyBitmap =
                    ImageUtils.createEmptyBitmap(
                            CONTENT_IMAGE_SIZE,
                            CONTENT_IMAGE_SIZE
                    )
            return ModelExecutionResult(
                    emptyBitmap, errorMessage = e.message!!
            )
        }
    }*/
    // Function for Interpreter
    fun executeOcrWithInterpreter(
        contentImage: Bitmap,
        context: Context
    ): IntArray {
        try {
            Log.i(TAG, "running models")

            fullExecutionTime = SystemClock.uptimeMillis()

            preProcessTime = SystemClock.uptimeMillis()
            // Creates inputs for reference.

            // Create an ImageProcessor with all ops required. For more ops, please
            // refer to the ImageProcessor Architecture.

            // Create an ImageProcessor with all ops required. For more ops, please
            // refer to the ImageProcessor Architecture.
            val imageProcessor = ImageProcessor.Builder()
                .add(
                    ResizeOp(
                        31,
                        200,
                        ResizeOp.ResizeMethod.BILINEAR
                    )
                )
                .add(NormalizeOp(0f, 255.0f))
                .build()

            Log.i(TAG, "after imageProcessor")
            // Create a TensorImage object. This creates the tensor of the corresponding
            // tensor type (flot32 in this case) that the TensorFlow Lite interpreter needs.

            // Create a TensorImage object. This creates the tensor of the corresponding
            // tensor type (flot32 in this case) that the TensorFlow Lite interpreter needs.
            var tImage = TensorImage(DataType.FLOAT32)

            // Analysis code for every frame
            // Preprocess the image

            // Analysis code for every frame
            // Preprocess the image
            tImage.load(androidGrayScale(contentImage))
            Log.i(TAG, "after loading")
            tImage = imageProcessor.process(tImage)
            Log.i(TAG, "after processing")

            // Create a container for the result and specify that this is not a quantized model.
            // Hence, the 'DataType' is defined as FLOAT32

            // Create a container for the result and specify that this is not a quantized model.
            // Hence, the 'DataType' is defined as float32
            val probabilityBuffer = TensorBuffer.createFixedSize(
                intArrayOf(1, 48),
                DataType.FLOAT32
            )
            Log.i(TAG, "after probability buffer")


            /*interpreterPredict.run(
                ImageUtils.bitmapToByteBuffer(
                    contentImage, CONTENT_IMAGE_WIDTH,
                    CONTENT_IMAGE_HEIGHT
                ), probabilityBuffer.buffer
            )*/

            interpreterPredict.run(tImage.buffer, probabilityBuffer.buffer)



            Log.i(TAG, "after running")

            preProcessTime = SystemClock.uptimeMillis() - preProcessTime

            stylePredictTime = SystemClock.uptimeMillis()


            stylePredictTime = SystemClock.uptimeMillis() - stylePredictTime

            Log.i(TAG, "Predict Time to run: $stylePredictTime")

            styleTransferTime = SystemClock.uptimeMillis()
            // Runs model inference and gets result.
            styleTransferTime = SystemClock.uptimeMillis() - styleTransferTime
            Log.d(TAG, "Style apply Time to run: $styleTransferTime")

            postProcessTime = SystemClock.uptimeMillis()
            postProcessTime = SystemClock.uptimeMillis() - postProcessTime

            fullExecutionTime = SystemClock.uptimeMillis() - fullExecutionTime
            Log.d(TAG, "Time to run everything: $fullExecutionTime")

            return probabilityBuffer.intArray
        } catch (e: Exception) {
            val exceptionLog = "something went wrong: ${e.message}"
            Log.e("EXECUTOR", exceptionLog)

            /*val emptyBitmap =
                ImageUtils.createEmptyBitmap(
                    CONTENT_IMAGE_SIZE,
                    CONTENT_IMAGE_SIZE
                )*/
            return intArrayOf()
        }
    }

    /*// Function for ML Binding
    fun executeOcrWithMLBinding(
        contentImage: Bitmap,
        context: Context
    ): IntArray {
        try {
            Log.i(TAG, "running models")

            fullExecutionTime = SystemClock.uptimeMillis()

            preProcessTime = SystemClock.uptimeMillis()
            // Creates inputs for reference.

            val loadedImage = TensorImage.fromBitmap(contentImage).tensorBuffer
            preProcessTime = SystemClock.uptimeMillis() - preProcessTime

            stylePredictTime = SystemClock.uptimeMillis()
            // The results of this inference could be reused given the style does not change
            // That would be a good practice in case this was applied to a video stream.
            // Runs model inference and gets result.
            Log.d(TAG, "Style Predict Time to run: $stylePredictTime")
            val outputsPredict = ocrFloat16Metadata.process(loadedImage)
            stylePredictTime = SystemClock.uptimeMillis() - stylePredictTime
            Log.d(TAG, "Style Predict Time to run: $stylePredictTime")

            styleTransferTime = SystemClock.uptimeMillis()
            // Runs model inference and gets result.
            styleTransferTime = SystemClock.uptimeMillis() - styleTransferTime
            Log.d(TAG, "Style apply Time to run: $styleTransferTime")

            postProcessTime = SystemClock.uptimeMillis()
            postProcessTime = SystemClock.uptimeMillis() - postProcessTime

            fullExecutionTime = SystemClock.uptimeMillis() - fullExecutionTime
            Log.d(TAG, "Time to run everything: $fullExecutionTime")

            return outputsPredict.arrayOutputAsTensorBuffer.intArray
        } catch (e: Exception) {
            val exceptionLog = "something went wrong: ${e.message}"
            Log.e("EXECUTOR", exceptionLog)

            *//*val emptyBitmap =
                ImageUtils.createEmptyBitmap(
                    CONTENT_IMAGE_SIZE,
                    CONTENT_IMAGE_SIZE
                )*//*
            return intArrayOf()
        }
    }

    */

    fun toGrayscale(bmpOriginal: Bitmap): Bitmap? {
        val height: Int = bmpOriginal.height
        val width: Int = bmpOriginal.width
        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmpGrayscale)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        val f = ColorMatrixColorFilter(cm)
        paint.colorFilter = f
        c.drawBitmap(bmpOriginal, 0f, 0f, paint)
        return bmpGrayscale
    }

    private fun getByteBufferNormalized(bitmap: Bitmap): ByteBuffer? {
        val width = bitmap.width
        val height = bitmap.height
        val mImgData: ByteBuffer = ByteBuffer
            .allocateDirect(4 * width * height)
        mImgData.order(ByteOrder.nativeOrder())
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (pixel in pixels) {
            mImgData.putFloat(Color.red(pixel).toFloat() / 255.0f)
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

    private fun androidGrayScale(bmpOriginal: Bitmap): Bitmap? {
        val width: Int
        val height: Int
        height = bmpOriginal.height
        width = bmpOriginal.width
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
