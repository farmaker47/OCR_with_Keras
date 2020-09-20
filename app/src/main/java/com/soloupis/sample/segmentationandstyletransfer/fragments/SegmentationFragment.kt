package com.soloupis.sample.segmentationandstyletransfer.fragments

import android.graphics.*
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.soloupis.sample.segmentationandstyletransfer.ImageUtils
import com.soloupis.sample.segmentationandstyletransfer.MainActivity
import com.soloupis.sample.segmentationandstyletransfer.R
import kotlinx.android.synthetic.main.fragment_selfie2segmentation.*
import kotlinx.coroutines.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter.ImageSegmenterOptions
import org.tensorflow.lite.task.vision.segmenter.OutputType
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [SegmentationFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 * This is where we show both the captured input image and the output image
 */
class SegmentationFragment : Fragment() {

    private val args: SegmentationFragmentArgs by navArgs()
    private lateinit var filePath: String
    private var finalBitmap: Bitmap? = null

    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(
        Dispatchers.Default + parentJob
    )

    private lateinit var imageSegmenter: ImageSegmenter
    private lateinit var scaledMaskBitmap: Bitmap
    var startTime: Long = 0L
    var inferenceTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true) // enable toolbar

        retainInstance = true
        filePath = args.rootDir
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_selfie2segmentation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val photoFile = File(filePath)

        Glide.with(imageview_input.context)
            .load(photoFile)
            .into(imageview_input)

        val selfieBitmap = BitmapFactory.decodeFile(filePath)
        coroutineScope.launch {
            val (outputBitmap, inferenceTime) = cropPersonFromPhoto(selfieBitmap)
            withContext(Dispatchers.Main) {
                updateUI(outputBitmap, inferenceTime)
                finalBitmap = outputBitmap
            }
        }
    }

    private fun cropPersonFromPhoto(bitmap: Bitmap): Pair<Bitmap?, Long> {
        try {
            // Initialization
            startTime = SystemClock.uptimeMillis()
            val options =
                ImageSegmenterOptions.builder().setOutputType(OutputType.CATEGORY_MASK).build()
            imageSegmenter =
                ImageSegmenter.createFromFileAndOptions(
                    requireActivity(),
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

    private fun updateUI(outputBitmap: Bitmap?, inferenceTime: Long) {
        progressbar.visibility = View.GONE
        imageview_output?.setImageBitmap(outputBitmap)
        inference_info.text = "Inference time: " + inferenceTime.toString() + "ms"
    }

    override fun onDestroy() {
        super.onDestroy()
        // clean up coroutine job
        parentJob.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> saveImageToSDCard(finalBitmap)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveImageToSDCard(bitmap: Bitmap?): String {

        val file = File(
            MainActivity.getOutputDirectory(requireContext()),
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + "_segmentation.jpg"
        )

        ImageUtils.saveBitmap(bitmap, file)
        Toast.makeText(context, "saved to " + file.absolutePath.toString(), Toast.LENGTH_SHORT)
            .show()

        return file.absolutePath

    }

    companion object {
        private const val TAG = "SegmentationFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

}