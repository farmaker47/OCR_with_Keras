package com.soloupis.sample.ocr_keras.fragments.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soloupis.sample.ocr_keras.databinding.FragmentOcrFragmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.viewmodel.ext.android.viewModel
import org.tensorflow.lite.support.common.FileUtil

class OcrFragment : Fragment(),
    SearchFragmentNavigationAdapter.SearchClickItemListener {

    // Koin inject ViewModel
    private val viewModel: OcrViewModel by viewModel()

    // DataBinding
    private lateinit var binding: FragmentOcrFragmentBinding

    // RecyclerView
    private lateinit var searchFragmentNavigationAdapter: SearchFragmentNavigationAdapter
    private var outputArray: LongArray = longArrayOf()
    private var inferenceFullTime = 0L
    private lateinit var labels: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        binding.recyclerViewImages.apply {
            setHasFixedSize(true)
            layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            adapter = searchFragmentNavigationAdapter

        }

        observeViewModel()

        labels = FileUtil.loadLabels(requireActivity(), "alphabets.txt")

        return binding.root
    }

    private fun observeViewModel() {

        // Observe ocr procedure
        viewModel.inferenceDone.observe(
            requireActivity(),
            Observer { loadingDone ->
                /*when (loadingDone) {
                    //true -> binding.progressbarStyle.visibility = View.GONE
                }*/
            }
        )

        // Observe ML kit inference time
        viewModel.inferenceTimeMlKit.observe(requireActivity(), Observer { time ->

            Log.i("ML_Kit_time", time.toString())

        })

    }

    companion object {
        private const val TAG = "OcrFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    override fun onListItemClick(itemIndex: Int, sharedImage: ImageView?, imagePath: String) {

        // Using Keras OCR
        executeOcr(imagePath)

        //getKoin().setProperty(getString(R.string.koinStyle), type)
        binding.imageviewOutput.setImageBitmap(
            getBitmapFromAsset(
                requireActivity(),
                "thumbnails/$imagePath"
            )
        )

        // Using ML Kit
        viewModel.performOcrWithMlKit(imagePath, requireActivity())

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
                Log.i("RESULT_OCR", outputArray.contentToString())

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


}