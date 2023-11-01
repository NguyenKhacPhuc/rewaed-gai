

package com.tcp.rewaed.ui.textdetector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.camera.CameraSourceConfig
import com.google.mlkit.vision.camera.CameraXSource
import com.google.mlkit.vision.camera.DetectionTaskCallback
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.tcp.rewaed.R
import java.util.Objects
import kotlin.collections.List
import timber.log.Timber

/** Live preview demo app for ML Kit APIs using CameraXSource API. */
@KeepName
class CameraXSourceDemoActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {
  private var previewView: PreviewView? = null
  private var graphicOverlay: GraphicOverlay? = null
  private var needUpdateGraphicOverlayImageSourceInfo = false
  private var lensFacing: Int = CameraSourceConfig.CAMERA_FACING_BACK
  private var cameraXSource: CameraXSource? = null
  private var customObjectDetectorOptions: CustomObjectDetectorOptions? = null
  private var targetResolution: Size? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Timber.tag(TAG).d("onCreate")
    setContentView(R.layout.activity_vision_cameraxsource_demo)
    previewView = findViewById(R.id.preview_view)
    if (previewView == null) {
      Timber.tag(TAG).d("previewView is null")
    }
    graphicOverlay = findViewById(R.id.graphic_overlay)
    if (graphicOverlay == null) {
      Timber.tag(TAG).d("graphicOverlay is null")
    }
    val facingSwitch = findViewById<ToggleButton>(R.id.facing_switch)
    facingSwitch.setOnCheckedChangeListener(this)
    val settingsButton = findViewById<ImageView>(R.id.settings_button)
    settingsButton.setOnClickListener {
      val intent = Intent(applicationContext, SettingsActivity::class.java)
      intent.putExtra(SettingsActivity.EXTRA_LAUNCH_SOURCE, SettingsActivity.LaunchSource.CAMERAXSOURCE_DEMO)
      startActivity(intent)
    }
  }

  override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
    if (lensFacing == CameraSourceConfig.CAMERA_FACING_FRONT) {
      lensFacing = CameraSourceConfig.CAMERA_FACING_BACK
    } else {
      lensFacing = CameraSourceConfig.CAMERA_FACING_FRONT
    }
    createThenStartCameraXSource()
  }

  public override fun onResume() {
    super.onResume()
    if (cameraXSource != null &&
        PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(this, localModel)
          .equals(customObjectDetectorOptions) &&
        PreferenceUtils.getCameraXTargetResolution(applicationContext, lensFacing) != null &&
        (Objects.requireNonNull(
          PreferenceUtils.getCameraXTargetResolution(applicationContext, lensFacing)
        ) == targetResolution)
    ) {
      if (ActivityCompat.checkSelfPermission(
          this,
          Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
      cameraXSource!!.start()
    } else {
      createThenStartCameraXSource()
    }
  }

  override fun onPause() {
    super.onPause()
    if (cameraXSource != null) {
      cameraXSource!!.stop()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (cameraXSource != null) {
      cameraXSource!!.stop()
    }
  }

  @SuppressLint("MissingPermission")
  private fun createThenStartCameraXSource() {
    if (cameraXSource != null) {
      cameraXSource!!.close()
    }
    customObjectDetectorOptions =
      PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
        getApplicationContext(),
        localModel
      )
    val objectDetector: ObjectDetector = ObjectDetection.getClient(customObjectDetectorOptions!!)
    val detectionTaskCallback: DetectionTaskCallback<List<DetectedObject>> =
      DetectionTaskCallback<List<DetectedObject>> { detectionTask ->
        detectionTask
          .addOnSuccessListener { results -> onDetectionTaskSuccess(results) }
          .addOnFailureListener { e -> onDetectionTaskFailure(e) }
      }
    val builder: CameraSourceConfig.Builder =
      CameraSourceConfig.Builder(getApplicationContext(), objectDetector, detectionTaskCallback)
        .setFacing(lensFacing)
    targetResolution =
      PreferenceUtils.getCameraXTargetResolution(getApplicationContext(), lensFacing)
    if (targetResolution != null) {
      builder.setRequestedPreviewSize(targetResolution!!.width, targetResolution!!.height)
    }
    cameraXSource = CameraXSource(builder.build(), previewView!!)
    needUpdateGraphicOverlayImageSourceInfo = true
    cameraXSource!!.start()
  }

  private fun onDetectionTaskSuccess(results: List<DetectedObject>) {
    graphicOverlay!!.clear()
    if (needUpdateGraphicOverlayImageSourceInfo) {
      val size: Size = cameraXSource!!.getPreviewSize()!!
      if (size != null) {
        Timber.tag(TAG).d("preview width: %s", size.width)
        Timber.tag(TAG).d("preview height: %s", size.height)
        val isImageFlipped =
          cameraXSource!!.getCameraFacing() == CameraSourceConfig.CAMERA_FACING_FRONT
        if (isPortraitMode) {
          // Swap width and height sizes when in portrait, since it will be rotated by
          // 90 degrees. The camera preview and the image being processed have the same size.
          graphicOverlay!!.setImageSourceInfo(size.height, size.width, isImageFlipped)
        } else {
          graphicOverlay!!.setImageSourceInfo(size.width, size.height, isImageFlipped)
        }
        needUpdateGraphicOverlayImageSourceInfo = false
      } else {
        Timber.tag(TAG).d("previewsize is null")
      }
    }
    Timber.tag(TAG).v("Number of object been detected: %s", results.size)
    for (`object` in results) {
      graphicOverlay!!.add(ObjectGraphic(graphicOverlay!!, `object`))
    }
    graphicOverlay!!.add(InferenceInfoGraphic(graphicOverlay!!))
    graphicOverlay!!.postInvalidate()
  }

  private fun onDetectionTaskFailure(e: Exception) {
    graphicOverlay!!.clear()
    graphicOverlay!!.postInvalidate()
    val error = "Failed to process. Error: " + e.localizedMessage
    Toast.makeText(
        graphicOverlay!!.getContext(),
        """
   $error
   Cause: ${e.cause}
      """.trimIndent(),
        Toast.LENGTH_SHORT
      )
      .show()
    Timber.tag(TAG).d(error)
  }

  private val isPortraitMode: Boolean
    private get() =
      (getApplicationContext().getResources().getConfiguration().orientation !==
        Configuration.ORIENTATION_LANDSCAPE)

  companion object {
    private const val TAG = "CameraXSourcePreview"
    private val localModel: LocalModel =
      LocalModel.Builder().setAssetFilePath("custom_models/object_labeler.tflite").build()
  }
}
