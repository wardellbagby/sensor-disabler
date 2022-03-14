package com.wardellbagby.sensordisabler.sensordetail

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnSliderTouchListener
import com.squareup.cycler.Recycler
import com.squareup.cycler.toDataSource
import com.squareup.workflow1.ui.LayoutRunner
import com.squareup.workflow1.ui.LayoutRunner.Companion.bind
import com.squareup.workflow1.ui.ViewEnvironment
import com.squareup.workflow1.ui.ViewFactory
import com.squareup.workflow1.ui.backPressedHandler
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.R.dimen
import com.wardellbagby.sensordisabler.recycler.HorizontalPaddingItemDecoration
import com.wardellbagby.sensordisabler.sensordetail.Row.MockableValueRow
import com.wardellbagby.sensordisabler.sensordetail.Row.ModificationTypeRow
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow.Rendering
import kotlin.math.max
import kotlin.math.min

sealed class Row {
  data class ModificationTypeRow(
    val title: String,
    val isSelected: Boolean,
    val onClick: () -> Unit
  ) : Row()

  data class MockableValueRow(
    val mockableValue: MockableValue,
    val onMockValueChanged: (MockableValue) -> Unit
  ) : Row()
}

class SensorDetailLayoutRunner(
  private val view: View
) : LayoutRunner<Rendering> {
  private val save: Button = view.findViewById(R.id.save)
  private val sensorDetails =
    Recycler.adopt<Row>(view.findViewById(R.id.sensor_list)) {
      row<ModificationTypeRow, MaterialRadioButton> {
        create(R.layout.modification_row) {
          this.view = MaterialRadioButton(view.context)
          this.view.isUseMaterialThemeColors = true
          bind { data ->
            this.view.text = data.title
            this.view.setOnCheckedChangeListener(null)
            this.view.isChecked = data.isSelected
            this.view.setOnCheckedChangeListener { _, _ -> data.onClick() }
          }
        }
      }

      row<MockableValueRow, ViewGroup> {
        create(R.layout.single_sensor_value_setting) {
          val label = this.view.findViewById<TextView>(R.id.label)
          val slider = this.view.findViewById<Slider>(R.id.value)

          bind { data ->
            val mockableValue = data.mockableValue
            label.text = data.mockableValue.title
            slider.apply {
              clearOnSliderTouchListeners()

              stepSize = (mockableValue.maximum - mockableValue.minimum) / 100
              valueFrom = mockableValue.minimum
              valueTo = mockableValue.maximum
              value = min(max(mockableValue.minimum, mockableValue.value), mockableValue.maximum)

              addOnSliderTouchListener(object : OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) = Unit
                override fun onStopTrackingTouch(slider: Slider) {
                  data.onMockValueChanged(mockableValue.copy(value = slider.value))
                }
              })
            }
          }
        }
      }
    }
      .also {
        it.view.addItemDecoration(
          HorizontalPaddingItemDecoration(
            it.view.resources.getDimensionPixelSize(dimen.recycler_horizontal_padding)
          )
        )
      }

  override fun showRendering(
    rendering: Rendering,
    viewEnvironment: ViewEnvironment
  ) {
    view.backPressedHandler = { rendering.onBack() }
    save.setOnClickListener { rendering.onSave() }
    sensorDetails.data = rendering.rows.toDataSource()
  }

  companion object : ViewFactory<Rendering> by
  bind(R.layout.sensor_detail_layout, ::SensorDetailLayoutRunner)
}