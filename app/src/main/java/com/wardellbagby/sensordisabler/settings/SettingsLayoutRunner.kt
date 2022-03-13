package com.wardellbagby.sensordisabler.settings

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import com.squareup.cycler.Recycler
import com.squareup.cycler.toDataSource
import com.squareup.workflow1.ui.*
import com.squareup.workflow1.ui.LayoutRunner.Companion.bind
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.settings.Row.*

private sealed class Row {
  data class ButtonRow(
    val title: String,
    val subtitle: String?,
    val onClick: () -> Unit
  ) : Row()

  data class CheckboxRow(
    val title: String,
    val subtitle: String?,
    val isChecked: Boolean,
    val onCheckChange: (isChecked: Boolean) -> Unit
  ) : Row()

  data class SectionHeader(val title: String) : Row()

  object Divider : Row()
}

class SettingsLayoutRunner(
  private val view: View
) : LayoutRunner<SettingsScreen> {
  private val toolbar: WorkflowViewStub = view.findViewById(R.id.toolbar_stub)
  private val settingsList =
    Recycler.adopt<Row>(view.findViewById(R.id.settings_list)) {
      row<ButtonRow, View> {
        create(R.layout.button_row) {
          val title = view.findViewById<TextView>(R.id.title)
          val subtitle = view.findViewById<TextView>(R.id.subtitle)
          bind { data ->
            title.text = data.title
            if (!data.subtitle.isNullOrBlank()) {
              subtitle.visibility = View.VISIBLE
              subtitle.text = data.subtitle
            } else {
              subtitle.visibility = View.GONE
            }
            view.setOnClickListener { data.onClick() }
          }
        }
      }
      row<CheckboxRow, View> {
        create(R.layout.checkbox_row) {
          val title = this.view.findViewById<TextView>(R.id.title)
          val subtitle = this.view.findViewById<TextView>(R.id.subtitle)
          val checkbox = this.view.findViewById<CheckBox>(R.id.checkbox)

          checkbox.isClickable = false

          bind { data ->
            view.setOnClickListener(null)
            title.text = data.title
            if (!data.subtitle.isNullOrBlank()) {
              subtitle.visibility = View.VISIBLE
              subtitle.text = data.subtitle
            } else {
              subtitle.visibility = View.GONE
            }
            checkbox.isChecked = data.isChecked
            view.setOnClickListener { data.onCheckChange(!data.isChecked) }
          }
        }
      }

      row<SectionHeader, TextView> {
        create(R.layout.section_header) {
          bind { data ->
            this.view.text = data.title
          }
        }
      }
      row<Divider, View> { create(layoutId = R.layout.divider, block = {}) }
    }

  override fun showRendering(
    rendering: SettingsScreen,
    viewEnvironment: ViewEnvironment
  ) {
    view.backPressedHandler = { rendering.onBack() }
    toolbar.update(rendering.toolbar, viewEnvironment)
    settingsList.data = rendering.sections
      .flatMap {
        listOf(SectionHeader(it.header)) + it.rows.map { settingsRow ->
          when (settingsRow) {
            is SettingsRow.ButtonRow ->
              ButtonRow(
                title = settingsRow.title,
                subtitle = settingsRow.subtitle,
                onClick = settingsRow.onClick
              )
            is SettingsRow.CheckboxRow ->
              CheckboxRow(
                title = settingsRow.title,
                subtitle = settingsRow.subtitle,
                isChecked = settingsRow.isChecked,
                onCheckChange = settingsRow.onCheckChanged
              )
          }
        }
      }
      .addDividers()
      .toDataSource()
  }

  private fun List<Row>.addDividers() =
    zip(mapIndexed { index, _ -> index })
      .flatMap { (row, index) ->
        when {
          index != 0 && row is SectionHeader -> listOf(Divider, row)
          index == lastIndex -> listOf(row, Divider)
          else -> listOf(row)
        }
      }

  companion object : ViewFactory<SettingsScreen> by bind(
    R.layout.settings_layout,
    ::SettingsLayoutRunner
  )
}