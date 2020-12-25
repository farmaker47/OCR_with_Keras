package com.soloupis.sample.ocr_keras.fragments.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.soloupis.sample.ocr_keras.ml.OcrFloat16Metadata
import com.soloupis.sample.ocr_keras.utils.ImageUtils
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.model.Model
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class ModelExecutionResult(
        val styledImage: Bitmap,
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
    //private var modelMlBindingPredict: MagentaArbitraryImageStylizationV1256Fp16Prediction1
    //private var modelMlBindingTransfer: MagentaArbitraryImageStylizationV1256Fp16Transfer1
    private var ocrFloat16Metadata: OcrFloat16Metadata
    private val interpreterPredict: Interpreter

    init {

        // ML binding set number of threads or GPU for accelerator
        /*val compatList = CompatibilityList()
        val options = if(compatList.isDelegateSupportedOnThisDevice) {
            Log.d(TAG, "This device is GPU Compatible ")
            Model.Options.Builder().setDevice(Model.Device.GPU).build()
        } else {
            Log.d(TAG, "This device is not GPU Incompatible ")
            Model.Options.Builder().setNumThreads(4).build()
        }*/

        val options = Model.Options.Builder().setNumThreads(4).build()
        //modelMlBindingPredict = MagentaArbitraryImageStylizationV1256Fp16Prediction1.newInstance(context, options)
        //modelMlBindingTransfer = MagentaArbitraryImageStylizationV1256Fp16Transfer1.newInstance(context, options)

        ocrFloat16Metadata = OcrFloat16Metadata.newInstance(context,options)

        // Interpreter
        interpreterPredict = getInterpreter(context, OCR_MODEL, false)

    }

    companion object {
        private const val TAG = "StyleTransferMExec"
        private const val STYLE_IMAGE_SIZE = 256
        private const val CONTENT_IMAGE_SIZE = 384
        private const val BOTTLENECK_SIZE = 100

        private const val OCR_MODEL = "ocr_float16.tflite"
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

            val loadedImage = TensorImage.fromBitmap(contentImage).tensorBuffer
            preProcessTime = SystemClock.uptimeMillis() - preProcessTime

            stylePredictTime = SystemClock.uptimeMillis()

            // Runs model inference and gets result.
            val outputsPredict = ocrFloat16Metadata.process(loadedImage)
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

            return outputsPredict.arrayOutputAsTensorBuffer.intArray
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

    // Function for ML Binding
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

            /*val emptyBitmap =
                ImageUtils.createEmptyBitmap(
                    CONTENT_IMAGE_SIZE,
                    CONTENT_IMAGE_SIZE
                )*/
            return intArrayOf()
        }
    }

    @Throws(IOException::class)
    private fun getInterpreter(
        context: Context,
        modelName: String,
        useGpu: Boolean = false
    ): Interpreter {
        val tfliteOptions = Interpreter.Options()

        // Use CPU threads or XNNPACK
        //tfliteOptions.setNumThreads(numberThreads)
        //tfliteOptions.setUseXNNPACK(true)

        /*gpuDelegate = null
        if (useGpu) {
            gpuDelegate = GpuDelegate()
            tfliteOptions.addDelegate(gpuDelegate)
        }*/

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

    private fun formatExecutionLog(): String {
        val sb = StringBuilder()
        sb.append("Input Image Size: $CONTENT_IMAGE_SIZE x $CONTENT_IMAGE_SIZE\n")
        sb.append("GPU enabled: $useGPU\n")
        sb.append("Number of threads: $numberThreads\n")
        sb.append("Pre-process execution time: $preProcessTime ms\n")
        sb.append("Predicting style execution time: $stylePredictTime ms\n")
        sb.append("Transferring style execution time: $styleTransferTime ms\n")
        sb.append("Post-process execution time: $postProcessTime ms\n")
        sb.append("Full execution time: $fullExecutionTime ms\n")
        return sb.toString()
    }

    fun close() {
        //modelMlBindingPredict.close()
        //modelMlBindingTransfer.close()
        ocrFloat16Metadata.close()
    }
}
