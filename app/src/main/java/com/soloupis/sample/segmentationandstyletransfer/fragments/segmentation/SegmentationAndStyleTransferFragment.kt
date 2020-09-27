package com.soloupis.sample.segmentationandstyletransfer.fragments.segmentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.soloupis.sample.segmentationandstyletransfer.ImageUtils
import com.soloupis.sample.segmentationandstyletransfer.MainActivity
import com.soloupis.sample.segmentationandstyletransfer.R
import com.soloupis.sample.segmentationandstyletransfer.databinding.FragmentSelfie2segmentationBinding
import kotlinx.android.synthetic.main.fragment_selfie2segmentation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [SegmentationAndStyleTransferFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 * This is where we show both the captured input image and the output image
 */
class SegmentationAndStyleTransferFragment : Fragment(),
    SearchFragmentNavigationAdapter.SearchClickItemListener {

    private val args: SegmentationAndStyleTransferFragmentArgs by navArgs()
    private lateinit var filePath: String
    private var finalBitmap: Bitmap? = null

    // Koin inject ViewModel
    private val viewModel: SegmentationAndStyleTransferViewModel by viewModel()

    // DataBinding
    private lateinit var binding: FragmentSelfie2segmentationBinding
    private lateinit var photoFile: File

    // RecyclerView
    private lateinit var mSearchFragmentNavigationAdapter: SearchFragmentNavigationAdapter

    //
    private lateinit var styleTransferModelExecutor: StyleTransferModelExecutor

    private lateinit var scaledBitmap: Bitmap
    private lateinit var selfieBitmap: Bitmap
    private var outputBitmapFinal: Bitmap? = null
    private var inferenceTime: Long = 0L

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
        binding = FragmentSelfie2segmentationBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModelXml = viewModel

        // RecyclerView setup
        binding.recyclerViewStyles.setHasFixedSize(true)
        binding.recyclerViewStyles.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        mSearchFragmentNavigationAdapter =
            SearchFragmentNavigationAdapter(
                requireActivity(),
                viewModel.currentList,
                this
            )
        binding.recyclerViewStyles.adapter = mSearchFragmentNavigationAdapter

        // Initialize class with Koin
        styleTransferModelExecutor = get()
        /*styleTransferModelExecutor.selectVideoQuality(5)
        styleTransferModelExecutor.firstSelectStyle(
            viewModel.stylename,
            5.0f,
            requireActivity()
        )*/
        getKoin().setProperty(getString(R.string.koinStyle), viewModel.stylename)


        observeViewModel()

        return binding.root
    }

    private fun observeViewModel() {

        viewModel.styledBitmap.observe(
            requireActivity(),
            Observer { resultImage ->
                if (resultImage != null) {
                    /*Glide.with(activity!!)
                        .load(resultImage.styledImage)
                        .fitCenter()
                        .into(binding.imageViewStyled)*/
                    binding.imageviewStyled.setImageBitmap(viewModel.cropBitmapWithMask(resultImage.styledImage, outputBitmapFinal))//selfieBitmap
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (filePath.startsWith("/storage")) {
            photoFile = File(filePath)

            Glide.with(imageview_input.context)
                .load(photoFile)
                .into(imageview_input)

            selfieBitmap = BitmapFactory.decodeFile(filePath)
            lifecycleScope.launch(Dispatchers.Default) {
                val (outputBitmap, inferenceTime) = viewModel.cropPersonFromPhoto(selfieBitmap)
                outputBitmapFinal = outputBitmap
                withContext(Dispatchers.Main) {
                    updateUI(outputBitmap, inferenceTime)
                    finalBitmap = outputBitmap
                }
            }
        } else {

            selfieBitmap =
                BitmapFactory.decodeStream(
                    requireActivity().contentResolver.openInputStream(
                        filePath.toUri()
                    )
                )

            Glide.with(imageview_input.context)
                .load(selfieBitmap)
                .into(imageview_input)

            lifecycleScope.launch(Dispatchers.Default) {
                val (outputBitmap, inferenceTime) = viewModel.cropPersonFromPhoto(selfieBitmap)
                outputBitmapFinal = outputBitmap
                withContext(Dispatchers.Main) {
                    updateUI(outputBitmap, inferenceTime)
                    finalBitmap = outputBitmap
                }
            }

        }

    }

    /*private fun cropPersonFromPhoto(bitmap: Bitmap): Pair<Bitmap?, Long> {
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
    }*/

    private fun updateUI(outputBitmap: Bitmap?, inferenceTime: Long) {
        progressbar.visibility = View.GONE
        imageview_output?.setImageBitmap(outputBitmap)
        inference_info.text = "Total process time: " + inferenceTime.toString() + "ms"

        //showStyledImage("mona.JPG")
    }

    private fun showStyledImage(style:String) {
        lifecycleScope.launch(Dispatchers.Default) {
            /*styleTransferModelExecutor.selectStyle(
                type,
                5.0f,
                scaledBitmap,
                requireActivity()
            )*/

            viewModel.setScaledBitmap(scaledBitmap)

            viewModel.onApplyStyle(
                requireActivity(), scaledBitmap, style, styleTransferModelExecutor
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // clean up coroutine job
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
        private const val TAG = "SegmentationAndStyleTransferFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val MODEL_WIDTH = 256
        const val MODEL_HEIGHT = 256
    }

    override fun onListItemClick(itemIndex: Int, sharedImage: ImageView?, type: String) {

        // Created scaled version of bitmap for model input.
        scaledBitmap = Bitmap.createScaledBitmap(
            selfieBitmap,
            MODEL_WIDTH,
            MODEL_HEIGHT, true
        )

        showStyledImage(type)
        getKoin().setProperty(getString(R.string.koinStyle), type)
        viewModel.setStyleName(type)

    }

}