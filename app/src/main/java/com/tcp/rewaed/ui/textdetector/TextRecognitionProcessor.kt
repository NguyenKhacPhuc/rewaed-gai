package com.tcp.rewaed.ui.textdetector

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.tcp.rewaed.ui.activities.MainActivity
import timber.log.Timber

/** Processor for the text detector demo. */
class TextRecognitionProcessor(
  private val context: Context,
  textRecognizerOptions: TextRecognizerOptionsInterface
) : VisionProcessorBase<Text>(context) {
  private val textRecognizer: TextRecognizer = TextRecognition.getClient(textRecognizerOptions)
  private val shouldGroupRecognizedTextInBlocks: Boolean =
    PreferenceUtils.shouldGroupRecognizedTextInBlocks(context)
  private val showLanguageTag: Boolean = PreferenceUtils.showLanguageTag(context)
  private val showConfidence: Boolean = PreferenceUtils.shouldShowTextConfidence(context)

  override fun stop() {
    super.stop()
    textRecognizer.close()
  }

  override fun detectInImage(image: InputImage): Task<Text> {
    return textRecognizer.process(image)
  }

  override fun onSuccess(text: Text, graphicOverlay: GraphicOverlay) {
    Timber.tag(TAG).d("On-device Text detection successful")
    logExtrasForTesting(text)
    graphicOverlay.add(
      TextGraphic(
        graphicOverlay,
        text,
        shouldGroupRecognizedTextInBlocks,
        showLanguageTag,
        showConfidence
      )
    )
    if (text.text.isNotEmpty()) {
      val intent = Intent(context, MainActivity::class.java)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      intent.putExtra(IS_FROM_TEXT_REG, true)
      intent.putExtra(TEXT_REG_VALUE, text.text)
      context.startActivity(intent)
    }
  }

  override fun onFailure(e: Exception) {
    Timber.tag(TAG).w("Text detection failed." + e)
  }

  companion object {
    private const val TAG = "TextRecProcessor"
    const val IS_FROM_TEXT_REG = "text_reg"
    const val TEXT_REG_VALUE = "text_reg_value"
    private fun logExtrasForTesting(text: Text?) {
      if (text != null) {
        Timber.tag(MANUAL_TESTING_LOG).v("Detected text has : " + text.textBlocks.size + " blocks")
        for (i in text.textBlocks.indices) {
          val lines = text.textBlocks[i].lines
          Timber.tag(MANUAL_TESTING_LOG)
            .v(String.format("Detected text block %d has %d lines", i, lines.size))
          for (j in lines.indices) {
            val elements = lines[j].elements
            Timber.tag(MANUAL_TESTING_LOG)
              .v(String.format("Detected text line %d has %d elements", j, elements.size))
            for (k in elements.indices) {
              val element = elements[k]
              Timber.tag(MANUAL_TESTING_LOG)
                .v(String.format("Detected text element %d says: %s", k, element.text))
              Timber.tag(MANUAL_TESTING_LOG).v(
                String.format(
                  "Detected text element %d has a bounding box: %s",
                  k,
                  element.boundingBox!!.flattenToString()
                )
              )
              Timber.tag(MANUAL_TESTING_LOG).v(
                String.format(
                  "Expected corner point size is 4, get %d",
                  element.cornerPoints!!.size
                )
              )
              for (point in element.cornerPoints!!) {
                Log.v(
                  MANUAL_TESTING_LOG,
                  String.format(
                    "Corner point for element %d is located at: x - %d, y = %d",
                    k,
                    point.x,
                    point.y
                  )
                )
              }
            }
          }
        }
      }
    }
  }
}
