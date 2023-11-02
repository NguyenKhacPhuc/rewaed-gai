

package com.tcp.rewaed.ui.textdetector

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tcp.rewaed.R
import com.tcp.rewaed.ui.activities.MainActivity
import java.io.IOException
import timber.log.Timber

/** Live preview demo for ML Kit APIs. */
@KeepName
class LivePreviewActivity :
    AppCompatActivity(), OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var selectedModel = TEXT_RECOGNITION_LATIN
    private lateinit var imageProcessor: TextRecognitionProcessor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).d("onCreate")
        setContentView(R.layout.activity_vision_live_preview)
        val captureButton = findViewById<ImageView>(R.id.takePictureIcon)
        preview = findViewById(R.id.preview_view)
        if (preview == null) {
            Timber.tag(TAG).d("Preview is null")
        }

        graphicOverlay = findViewById(R.id.graphic_overlay)
        if (graphicOverlay == null) {
            Timber.tag(TAG).d("graphicOverlay is null")
        }

        val options: MutableList<String> = ArrayList()
        options.add(TEXT_RECOGNITION_LATIN)

        // Creating adapter for spinner
        val dataAdapter = ArrayAdapter(this, R.layout.spinner_style, options)

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // attaching data adapter to spinner
        createCameraSource(selectedModel)
        captureButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra(TextRecognitionProcessor.IS_FROM_TEXT_REG, true)
            intent.putExtra(TextRecognitionProcessor.TEXT_REG_VALUE, imageProcessor.result)
            startActivity(intent)
        }
    }

    @Synchronized
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        selectedModel = parent?.getItemAtPosition(pos).toString()
        Timber.tag(TAG).d("Selected model: %s", selectedModel)
        preview?.stop()
        createCameraSource(selectedModel)
        startCameraSource()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing.
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        Timber.tag(TAG).d("Set facing")
        if (cameraSource != null) {
            if (isChecked) {
                cameraSource?.setFacing(CameraSource.CAMERA_FACING_FRONT)
            } else {
                cameraSource?.setFacing(CameraSource.CAMERA_FACING_BACK)
            }
        }
        preview?.stop()
        startCameraSource()
    }

    private fun createCameraSource(model: String) {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(this, graphicOverlay)
        }
        Timber.tag(TAG).i("Using on-device Text recognition Processor for Latin and Latin")
        imageProcessor = TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build(), true)

        cameraSource!!.setMachineLearningFrameProcessor(imageProcessor)

    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Timber.tag(TAG).d("resume: Preview is null")
                }
                if (graphicOverlay == null) {
                    Timber.tag(TAG).d("resume: graphOverlay is null")
                }
                preview!!.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                Timber.tag(TAG).e(e, "Unable to start camera source.")
                cameraSource!!.release()
                cameraSource = null
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        Timber.tag(TAG).d("onResume")
        createCameraSource(selectedModel)
        startCameraSource()
    }

    /** Stops the camera. */
    override fun onPause() {
        super.onPause()
        preview?.stop()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (cameraSource != null) {
            cameraSource?.release()
        }
    }

    companion object {
        private const val TEXT_RECOGNITION_LATIN = "Text Recognition Latin"

        private const val TAG = "LivePreviewActivity"
    }
}
