/** Tom Nguyen: I wrote this file and it's corresponding xml files. **/
/**
 * Shane Fretwell: I modified the try block of annotate() to be agnostic of which firebase endpoint
 * is being called, which makes the code base more flexible if we wanted to add endpoints in the
 * future
 */

package edu.uw.minh2804.rekognition.fragments

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import edu.uw.minh2804.rekognition.MainActivity.Companion.CONNECTION_TIMEOUT_IN_SECONDS
import edu.uw.minh2804.rekognition.R
import edu.uw.minh2804.rekognition.extensions.isPermissionGranted
import edu.uw.minh2804.rekognition.extensions.requestPermission
import edu.uw.minh2804.rekognition.services.*
import edu.uw.minh2804.rekognition.stores.*
import edu.uw.minh2804.rekognition.stores.Annotation
import edu.uw.minh2804.rekognition.viewmodels.AccessibilityViewModel
import kotlinx.coroutines.*

// This exception is used to throw when there is no internet connection.
class InternetNotFoundException : Exception("Internet not detected")

// This fragment is responsible for handling image processing and annotation of the captured photo.
class AnnotationFragment : Fragment(R.layout.fragment_annotation) {
    private lateinit var annotationStore: AnnotationStore
    private lateinit var thumbnailStore: ThumbnailStore
    private val model: AccessibilityViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireFirebaseOrIgnore()

        // Initialize local storage endpoints
        annotationStore = AnnotationStore(requireActivity())
        thumbnailStore = ThumbnailStore(requireActivity())

        // Annotate newly captured photos
        model.capturedPhoto.observe(this) {  if (!it.isProcessed) annotate(it) }
    }

    private fun annotate(output: CameraOutput) {
        lifecycleScope.launch {
            val id = output.id
            val thumbnail = Thumbnail(output.image)
            launch { thumbnailStore.save(id, thumbnail) }
            if (!isInternetEnable()) {
                model.onImageAnnotateFailed(InternetNotFoundException())
                return@launch
            }
            model.onImageAnnotating(getString(R.string.camera_output_on_processing))
            try {
                // If it takes more than CONNECTION_TIMEOUT_IN_SECONDS to retrieve the result, then a TimeoutCancellationException will be thrown.
                withTimeout(1000 * CONNECTION_TIMEOUT_IN_SECONDS) {
                    context ?: return@withTimeout
                    val result = output.requestAnnotator.annotate(thumbnail.bitmap)
                    val formattedResult = output.requestAnnotator.onAnnotated(result)
                    if (formattedResult != null) {
                        model.onImageAnnotated(formattedResult)
                        annotationStore.save(id, Annotation(result)) // REFACTOR why storing an unformatted result instead of formatted result?
                    } else {
                        val errorToDisplay = getString(
                            R.string.camera_output_result_not_found,
                            output.requestAnnotator.getIdentifierText(requireContext())
                        )
                        model.onImageAnnotated(errorToDisplay)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                model.onImageAnnotateFailed(getString(R.string.internal_error_message))
            }
            output.isProcessed = true
        }
    }

    // Get internet permission, and if internet is available, authenticate with firebase
    private fun requireFirebaseOrIgnore() {
        lifecycleScope.launch {
            val requiredPermission = Manifest.permission.INTERNET
            if (!requireActivity().isPermissionGranted(requiredPermission)) {
                requireActivity().requestPermission(requiredPermission)
            }
            if (isInternetEnable() && !FirebaseAuthService.isAuthenticated()) {
                FirebaseAuthService.signIn()
            }
        }
    }

    // See more: https://developer.android.com/training/monitoring-device-state/connectivity-status-type
    private fun isInternetEnable(): Boolean {
        val cm = requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork != null
    }

    companion object {
        private val TAG = AnnotationFragment::class.simpleName
    }
}