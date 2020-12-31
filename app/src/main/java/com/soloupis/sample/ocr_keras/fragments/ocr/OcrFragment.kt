package com.soloupis.sample.ocr_keras.fragments.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.soloupis.sample.ocr_keras.MainActivity
import com.soloupis.sample.ocr_keras.R
import com.soloupis.sample.ocr_keras.databinding.FragmentOcrFragmentBinding
import com.soloupis.sample.ocr_keras.fragments.StyleFragment
import com.soloupis.sample.ocr_keras.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.viewmodel.ext.android.viewModel
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [OcrFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 * This is where we show both the captured input image and the output image
 */
class OcrFragment : Fragment(),
        SearchFragmentNavigationAdapter.SearchClickItemListener,
        StyleFragment.OnListFragmentInteractionListener {

    private lateinit var filePath: String
    private var finalBitmap: Bitmap? = null
    private var finalBitmapWithStyle: Bitmap? = null

    // Koin inject ViewModel
    private val viewModel: OcrViewModel by viewModel()

    // DataBinding
    private lateinit var binding: FragmentOcrFragmentBinding
    private lateinit var photoFile: File

    // RecyclerView
    private lateinit var searchFragmentNavigationAdapter: SearchFragmentNavigationAdapter

    //
    private lateinit var ocrModelExecutor: OcrModelExecutor

    private lateinit var scaledBitmap: Bitmap
    private lateinit var selfieBitmap: Bitmap
    private lateinit var loadedBitmap: Bitmap


    private var outputArray: LongArray = longArrayOf()
    private var inferenceFullTime = 0L
    private lateinit var labels: List<String>


    private var outputBitmapFinal: Bitmap? = null
    private var inferenceTime: Long = 0L
    private val stylesFragment: StyleFragment = StyleFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true) // enable toolbar

        retainInstance = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentOcrFragmentBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModelXml = viewModel

        // RecyclerView setup
        searchFragmentNavigationAdapter =
                SearchFragmentNavigationAdapter(
                        requireActivity(),
                        viewModel.currentList,
                        this
                )
        binding.recyclerViewStyles.apply {
            setHasFixedSize(true)
            layoutManager =
                    LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            adapter = searchFragmentNavigationAdapter

        }

        // Initialize class with Koin
        ocrModelExecutor = get()

        observeViewModel()

        labels = FileUtil.loadLabels(requireActivity(), "alphabets.txt")

        // Click on Style picker
        /*binding.chooseStyleTextView.setOnClickListener {
            stylesFragment.show(requireActivity().supportFragmentManager, "StylesFragment")
        }*/

        // Listeners for toggle buttons
        /*binding.imageToggleLeft.setOnClickListener {

            // Make input ImageView visible
            binding.imageviewInput.visibility = View.VISIBLE
            // Make output Image gone
            binding.imageviewOutput.visibility = View.GONE
        }
        binding.imageToggleRight.setOnClickListener {

            // Make input ImageView gone
            binding.imageviewInput.visibility = View.GONE
            // Make output Image visible
            binding.imageviewOutput.visibility = View.VISIBLE
        }*/

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

                        // Set this to use with save function
                        /*finalBitmapWithStyle = viewModel.cropBitmapWithMaskForStyle(
                            outputBitmapFinal
                        )*/

                        /*binding.imageviewStyled.setImageBitmap(
                            viewModel.cropBitmapWithMaskForStyle(
                                outputBitmapFinal
                            )
                        )*///selfieBitmap
                    }
                }
        )

        // Observe style transfer procedure
        viewModel.inferenceDone.observe(
                requireActivity(),
                Observer { loadingDone ->
                    when (loadingDone) {
                        //true -> binding.progressbarStyle.visibility = View.GONE
                    }
                }
        )

        viewModel.totalTimeInference.observe(
                requireActivity(),
                Observer { time ->
                    //binding.inferenceInfoStyle.text = "Total process time: ${time}ms"
                }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    private fun updateUI(outputBitmap: Bitmap?, inferenceTime: Long) {
        binding.progressbar.visibility = View.GONE
        binding.imageviewInput.visibility = View.INVISIBLE
        Glide.with(binding.imageviewOutput.context)
                .load(outputBitmap)
                .fitCenter()
                .into(binding.imageviewOutput)
        //imageview_output?.setImageBitmap(outputBitmap)
        //binding.inferenceInfo.text = "Total process time: " + inferenceTime.toString() + "ms"

        //showStyledImage("mona.JPG")
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
            R.id.action_save -> saveImageToSDCard(finalBitmapWithStyle)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveImageToSDCard(bitmap: Bitmap?): String {

        val file = File(
                MainActivity.getOutputDirectory(requireContext()),
                SimpleDateFormat(
                        FILENAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()) + "_segmentation_and_style_transfer.jpg"
        )

        ImageUtils.saveBitmap(bitmap, file)
        Toast.makeText(context, "saved to " + file.absolutePath.toString(), Toast.LENGTH_SHORT)
                .show()

        return file.absolutePath

    }

    companion object {
        private const val TAG = "OcrFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val MODEL_WIDTH = 256
        const val MODEL_HEIGHT = 256
    }

    override fun onListItemClick(itemIndex: Int, sharedImage: ImageView?, imagePath: String) {

        // Upon click show progress bar
        //binding.progressbarStyle.visibility = View.VISIBLE
        // make placeholder gone
        //binding.imageviewPlaceholder.visibility = View.GONE

        // Created scaled version of bitmap for model input.
        /*scaledBitmap = Bitmap.createScaledBitmap(
            selfieBitmap,
            MODEL_WIDTH,
            MODEL_HEIGHT, true
        )*/

        executeOcr(imagePath)
        //getKoin().setProperty(getString(R.string.koinStyle), type)
        binding.imageviewOutput.setImageBitmap(getBitmapFromAsset(requireActivity(), "thumbnails/$imagePath"))

    }

    private fun getBitmapFromAsset(context: Context, path: String): Bitmap =
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }

    private fun executeOcr(path: String): Pair<LongArray, Long> {

        lifecycleScope.launch(Dispatchers.Default) {

            val (longArray, inferenceTime) = viewModel.onClickPerformOcr(
                    requireActivity(), path
            )
            withContext(Dispatchers.Main) {
                outputArray = longArray
                inferenceFullTime = inferenceTime
                Log.e("RESULT_OCR", outputArray.contentToString())

                val sb: StringBuilder = StringBuilder()
                for (i in outputArray.indices) {
                    if (outputArray[i].toString() != "-1") {
                        sb.append(labels[outputArray[i].toInt()])
                    }
                }

                binding.inferenceTime.text = "Inference Time: ${inferenceFullTime}ms"
                binding.textViewOcrResult.text = sb.toString()

            }
        }

        return Pair(outputArray, inferenceFullTime)
    }

    override fun onListFragmentInteraction(item: String) {
    }

    fun methodToStartStyleTransfer(item: String) {

        stylesFragment.dismiss()

        scaledBitmap = Bitmap.createScaledBitmap(
                selfieBitmap,
                MODEL_WIDTH,
                MODEL_HEIGHT, true
        )

        executeOcr(item)
        getKoin().setProperty(getString(R.string.koinStyle), item)
    }
}