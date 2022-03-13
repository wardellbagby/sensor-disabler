package com.wardellbagby.sensordisabler.sensordetail

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.squareup.workflow1.ui.*
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.views.addRow
import com.wardellbagby.sensordisabler.views.dp

private fun ViewGroup.createLabelView(label: String) = TextView(context).apply {
  text = label
  updatePadding(right = 4.dp)
}

data class InfoRendering(
  val name: String,
  val type: String,
  val vendor: String,
  val range: String,
  val description: String,
  val onClose: () -> Unit
) : AndroidViewRendering<InfoRendering> {
  override val viewFactory = BuilderViewFactory(
    type = InfoRendering::class
  ) { initialRendering, initialViewEnvironment, contextForNewView, _ ->
    val nameValue = TextView(contextForNewView)
    val typeValue = TextView(contextForNewView)
    val vendorValue = TextView(contextForNewView)
    val rangeValue = TextView(contextForNewView)

    val description = TextView(contextForNewView).apply {
      updatePadding(bottom = 16.dp)
    }
    val close = MaterialButton(contextForNewView).apply {
      text = context.getString(R.string.close)
    }
    LinearLayout(contextForNewView).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(16.dp)
      addView(TableLayout(context).apply {
        updatePadding(bottom = 16.dp)
        addRow(createLabelView(context.getString(R.string.sensor_info_name_label)), nameValue)
        addRow(createLabelView(context.getString(R.string.sensor_info_type_label)), typeValue)
        addRow(createLabelView(context.getString(R.string.sensor_info_vendor_label)), vendorValue)
        addRow(createLabelView(context.getString(R.string.sensor_info_range_label)), rangeValue)
      })
      addView(description)
      addView(close)

      fun update(rendering: InfoRendering, viewEnvironment: ViewEnvironment) {
        nameValue.text = rendering.name
        typeValue.text = rendering.type
        vendorValue.text = rendering.vendor
        rangeValue.text = rendering.range
        description.text = rendering.description

        close.setOnClickListener {
          rendering.onClose()
        }
        backPressedHandler = rendering.onClose
      }

      bindShowRendering(initialRendering, initialViewEnvironment, ::update)
    }
  }
}