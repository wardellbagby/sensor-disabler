package com.wardellbagby.sensordisabler.settings.filtering.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.squareup.workflow1.ui.AndroidViewRendering
import com.squareup.workflow1.ui.backPressedHandler
import com.squareup.workflow1.ui.compose.composeViewFactory
import com.wardellbagby.sensordisabler.images.AppIconRequest
import com.wardellbagby.sensordisabler.settings.filtering.settings.FilterSettingsWorkflow.ApplicationData
import com.wardellbagby.sensordisabler.util.FilterType
import com.wardellbagby.sensordisabler.views.CompositionRoot

// TODO Extract this into a string resource
private val FilterType.label: String
  get() = when (this) {
    FilterType.None -> error("Can't create label for None FilterType.")
    FilterType.Allow -> "allow"
    FilterType.Deny -> "deny"
  }

data class SelectableSensor(
  val name: String,
  val isChecked: Boolean,
  val onCheckedChanged: (isChecked: Boolean) -> Unit
)

data class ViewingApplicationSettingsScreen(
  val onBack: () -> Unit,
  val filterType: FilterType,
  val applicationData: ApplicationData,
  val sensors: List<SelectableSensor>
) : AndroidViewRendering<ViewingApplicationSettingsScreen> {
  override val viewFactory =
    composeViewFactory<ViewingApplicationSettingsScreen> { rendering, _ ->
      LocalView.current.backPressedHandler = rendering.onBack
      CompositionRoot {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(horizontal = 32.dp)
        ) {

          AsyncImage(
            model = AppIconRequest(rendering.applicationData.packageName),
            contentDescription = rendering.applicationData.label.toString(),
            modifier = Modifier
              .padding(16.dp)
              .size(64.dp)
          )
          Text(text = "Sensor Disabler will ${rendering.filterType.label} ${rendering.applicationData.label}'s access to the true values of the selected sensors below")
          Divider(Modifier.padding(vertical = 4.dp))

          LazyColumn(Modifier.fillMaxWidth()) {
            items(rendering.sensors) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = it.isChecked, onCheckedChange = it.onCheckedChanged)
                Text(text = it.name)
              }
            }
          }
        }
      }
    }
}