package com.soloupis.sample.ocr_keras.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.navigation.fragment.findNavController
import com.soloupis.sample.ocr_keras.R

/**
 * A simple [Fragment] subclass that handles camera permission request
 */
class PermissionsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (allPermissionsGranted(
                requireContext()
            )
        ) {
            findNavController().navigate(R.id.action_permissions_to_camera)
        } else {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted(
                    requireContext()
                )
            ) {
                findNavController().navigate(R.id.action_permissions_to_camera)
            } else {
                Toast.makeText(
                    context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()

                // Inform and finish activity
                activity?.finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        fun allPermissionsGranted(context: Context) = REQUIRED_PERMISSIONS.all {
            checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}