package com.wardellbagby.sensordisabler.sensorlist

import android.graphics.Color
import android.graphics.PorterDuff.Mode.SRC_ATOP
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.squareup.cycler.Recycler
import com.squareup.cycler.toDataSource
import com.squareup.workflow1.ui.LayoutRunner
import com.squareup.workflow1.ui.LayoutRunner.Companion.bind
import com.squareup.workflow1.ui.ViewEnvironment
import com.squareup.workflow1.ui.ViewFactory
import com.squareup.workflow1.ui.backPressedHandler
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow.Rendering
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow.Rendering.SensorData

class SensorListLayoutRunner(private val view: View) : LayoutRunner<Rendering> {
  private val sensorList =
    Recycler.adopt<SensorData>(view.findViewById(R.id.sensor_list)) {
      row<SensorData, TextView> {
        create(R.layout.sensor_list_item) {
          bind { data ->
            val textView = this.view
            textView.text = data.title
            textView.setOnClickListener { data.onClick() }
            if (data.dangerous) {
              textView.setDangerousIcon()
            } else textView.clearDangerousIcon()
          }
        }
      }
    }

  override fun showRendering(
    rendering: Rendering,
    viewEnvironment: ViewEnvironment
  ) {
    view.backPressedHandler = rendering.onBack
    sensorList.data = rendering.sensorData.toDataSource()
  }

  companion object : ViewFactory<Rendering> by bind(
    R.layout.sensor_list_layout, ::SensorListLayoutRunner
  )
}

private fun TextView.setDangerousIcon() {
  val drawable = ContextCompat.getDrawable(context, R.drawable.ic_error_outline_white_24dp)!!
  drawable.setColorFilter(Color.RED, SRC_ATOP)
  setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
}

private fun TextView.clearDangerousIcon() {
  setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
}