package com.wardellbagby.sensordisabler.settings.filtering.type

import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import com.squareup.workflow1.ui.AndroidViewRendering
import com.squareup.workflow1.ui.LayoutRunner
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.util.FilterType

data class FilterTypeScreen(
  val filterType: FilterType,
  val onChange: (FilterType) -> Unit,
  val onSave: () -> Unit
) : AndroidViewRendering<FilterTypeScreen> {
  override val viewFactory =
    LayoutRunner.bind<FilterTypeScreen>(R.layout.filter_type_layout) { view ->
      val group = view.findViewById<RadioGroup>(R.id.filter_type_group)
      val none = view.findViewById<RadioButton>(R.id.none_checkbox)
      val allow = view.findViewById<RadioButton>(R.id.allow_checkbox)
      val deny = view.findViewById<RadioButton>(R.id.deny_checkbox)
      val save = view.findViewById<Button>(R.id.save)
      LayoutRunner { rendering, _ ->
        group.setOnCheckedChangeListener(null)
        when (rendering.filterType) {
          FilterType.None -> none.isChecked = true
          FilterType.Allow -> allow.isChecked = true
          FilterType.Deny -> deny.isChecked = true
        }
        group.setOnCheckedChangeListener { _, id ->
          when (id) {
            R.id.none_checkbox -> onChange(FilterType.None)
            R.id.allow_checkbox -> onChange(FilterType.Allow)
            R.id.deny_checkbox -> onChange(FilterType.Deny)
            else -> error("Invalid ID: $id")
          }
        }
        save.setOnClickListener { rendering.onSave() }
      }
    }

}