/** Tom Nguyen: I wrote this file and it's corresponding xml files. **/
/**
 * Shane Fretwell: I implemented the state in this file that keeps track of what tab is selected,
 * and Tom made sure this state wasn't lost when landscape mode is engaged
 * I also adapted the xml for this fragment for landscape mode, which involved substantial but
 * fruitless research into vertical tab layouts and other methods of switching between Text and
 * Object detection
 */

package edu.uw.minh2804.rekognition.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import edu.uw.minh2804.rekognition.R
import edu.uw.minh2804.rekognition.services.FirebaseFunctionsService
import edu.uw.minh2804.rekognition.services.FirebaseFunctionsService.Annotator
import edu.uw.minh2804.rekognition.stores.*
import edu.uw.minh2804.rekognition.viewmodels.AccessibilityViewModel
import java.io.File
import kotlinx.coroutines.launch

// Encapsulates the state required to store an image locally, with metadata
data class CameraOutput(val id: String, val image: File, val requestAnnotator: Annotator, var isProcessed: Boolean = false)

// This fragment is responsible for handling user inputs and updating it to the view model.
// User input includes selecting one of the detection options and taking picture.
class AccessibilityFragment : Fragment(R.layout.fragment_accessibility) {
    private lateinit var photoStore: PhotoStore
    private val model: AccessibilityViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoStore = PhotoStore(requireActivity())

        val captureButton = view.findViewById<ImageButton>(R.id.button_camera_capture)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout_annotation_options)

        // Direct captured photos to the endpoint corresponding to the selected tab
        captureButton.setOnClickListener {
            val selectedTab = tabLayout.getTabAt(tabLayout.selectedTabPosition)
            val currentAnnotator = FirebaseFunctionsService.getAnnotatorForSelectedTab(requireContext(), tabLayout)
            if (currentAnnotator != null) takePhoto(currentAnnotator)
            else Log.e(TAG, "Selected tab ${selectedTab?.text} not matched to a computer vision endpoint")
        }

        // Keep track of selected tab in ViewModel for landscape consistency
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { model.onTabPositionChanged(tab!!.position) }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        model.tabPosition.observe(this) { tabLayout.selectTab(tabLayout.getTabAt(it)) }
    }

    // Take a photo and generate its metadata and local file. Then, pass the CameraOutput to ViewModel
    // If an exception is raised, an error message is displayed to the user
    private fun takePhoto(mode: Annotator) {
        lifecycleScope.launch {
            val camera = childFragmentManager.fragments[0] as CameraFragment
            val outputFile = photoStore.createOutputFile()
            val id = outputFile.nameWithoutExtension
            try {
                // Take photo and save it to local storage
                camera.takePhoto(outputFile.outputStream())
                photoStore.save(id, Photo(outputFile))
                model.onCameraCaptured(CameraOutput(id, outputFile, mode))
            } catch (e: CameraNotDetectedException) {
                model.onCameraCaptureFailed(e)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                model.onCameraCaptureFailed(getString(R.string.internal_error_message))
            }
        }
    }

    companion object {
        private val TAG = AccessibilityFragment::class.simpleName
    }
}