package com.wardellbagby.sensordisabler.settings

import com.wardellbagby.sensordisabler.toolbar.ToolbarRendering

data class SettingsScreen(
  val toolbar: ToolbarRendering,
  val sections: List<SettingsSection>,
  val onBack: () -> Unit
)