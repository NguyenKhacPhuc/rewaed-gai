

package com.tcp.rewaed.ui.textdetector

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.tcp.rewaed.R
import timber.log.Timber

/** Demo app chooser which allows you pick from all available testing Activities. */
class ChooserActivity :
  AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback, OnItemClickListener {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Timber.tag(TAG).d("onCreate")
    setContentView(R.layout.activity_chooser)

    // Set up ListView and Adapter
    val listView = findViewById<ListView>(R.id.test_activity_list_view)
    val adapter = MyArrayAdapter(this, android.R.layout.simple_list_item_2, CLASSES)
    adapter.setDescriptionIds(DESCRIPTION_IDS)
    listView.adapter = adapter
    listView.onItemClickListener = this
  }

  override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
    val clicked = CLASSES[position]
    startActivity(Intent(this, clicked))
  }

  private class MyArrayAdapter(
    private val ctx: Context,
    resource: Int,
    private val classes: Array<Class<*>>
  ) : ArrayAdapter<Class<*>>(ctx, resource, classes) {
    private var descriptionIds: IntArray? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
      var view = convertView

      if (convertView == null) {
        val inflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(android.R.layout.simple_list_item_2, null)
      }

      (view!!.findViewById<View>(android.R.id.text1) as TextView).text = if (position == 0) {
        "Live Preview"
      } else {
        "Image"
      }
      descriptionIds?.let {
        (view.findViewById<View>(android.R.id.text2) as TextView).setText(it[position])
      }

      return view
    }

    fun setDescriptionIds(descriptionIds: IntArray) {
      this.descriptionIds = descriptionIds
    }
  }

  companion object {
    private const val TAG = "ChooserActivity"
    private val CLASSES =
      arrayOf<Class<*>>(
        LivePreviewActivity::class.java,
        StillImageActivity::class.java
      )
    private val DESCRIPTION_IDS =
      intArrayOf(
        R.string.desc_camera_source_activity,
        R.string.desc_still_image_activity
      )
  }
}
