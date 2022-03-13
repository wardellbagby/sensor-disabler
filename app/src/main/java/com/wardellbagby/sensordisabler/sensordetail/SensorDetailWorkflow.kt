package com.wardellbagby.sensordisabler.sensordetail

import android.content.Context
import android.hardware.Sensor
import android.os.Parcelable
import com.squareup.workflow1.Snapshot
import com.squareup.workflow1.StatefulWorkflow
import com.squareup.workflow1.action
import com.squareup.workflow1.ui.toParcelable
import com.squareup.workflow1.ui.toSnapshot
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.modals.DualLayer
import com.wardellbagby.sensordisabler.modals.ModalScreen
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow.*
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow.Output.BackPressed
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow.Output.Saved
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow.State.EditingSensor
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow.State.ShowingInfo
import com.wardellbagby.sensordisabler.toolbar.NavigationIcon.MENU
import com.wardellbagby.sensordisabler.toolbar.ToolbarAction
import com.wardellbagby.sensordisabler.toolbar.ToolbarProps
import com.wardellbagby.sensordisabler.util.ModificationType
import com.wardellbagby.sensordisabler.util.SensorUtil
import com.wardellbagby.sensordisabler.util.displayName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class SensorDetailWorkflow
@Inject constructor(
  @ApplicationContext private val androidContext: Context
) : StatefulWorkflow<Props, State, Output, DualLayer<Rendering>>() {
  data class ModifiableSensor(
    val sensor: Sensor,
    val modificationType: ModificationType,
    val defaultMockableValues: List<MockableValue>
  )

  data class Props(
    val sensor: Sensor,
    val modificationType: ModificationType,
    val defaultMockableValues: List<MockableValue>
  )

  sealed class State : Parcelable {
    abstract val modificationType: ModificationType

    @Parcelize
    data class EditingSensor(
      override val modificationType: ModificationType
    ) : State()

    @Parcelize
    data class ShowingInfo(
      override val modificationType: ModificationType
    ) : State()
  }

  sealed class Output {
    object BackPressed : Output()
    data class Saved(val modificationType: ModificationType) : Output()
  }

  data class Rendering(
    val toolbarProps: ToolbarProps,
    val rows: List<Row>,
    val modificationType: ModificationType,
    val onUpdatedModificationType: (ModificationType) -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit
  )

  override fun onPropsChanged(
    old: Props,
    new: Props,
    state: State
  ): State = EditingSensor(new.modificationType)

  override fun initialState(
    props: Props,
    snapshot: Snapshot?
  ): State {
    return snapshot?.toParcelable() ?: EditingSensor(props.modificationType)
  }

  override fun render(
    renderProps: Props,
    renderState: State,
    context: RenderContext
  ): DualLayer<Rendering> {
    val baseRendering = Rendering(
      toolbarProps = renderProps.sensor.toolbarProps(context),
      modificationType = renderState.modificationType,
      onUpdatedModificationType = context.eventHandler { modificationType ->
        state = EditingSensor(modificationType)
      },
      onSave = context.eventHandler {
        setOutput(Saved(renderState.modificationType))
      },
      onBack = { action { setOutput(BackPressed) } },
      rows = generateRows(renderState, renderProps, context)
    )

    return when (renderState) {
      is ShowingInfo -> DualLayer(
        base = baseRendering,
        // TODO Extract message into string resource
        modal = ModalScreen(
          InfoRendering(
            name = renderProps.sensor.displayName,
            type = SensorUtil.getHumanStringType(renderProps.sensor) ?: "Unknown",
            vendor = renderProps.sensor.vendor,
            range = renderProps.sensor.maximumRange.toString(),
            description = SensorUtil.getDescription(renderProps.sensor),
            onClose = context.eventHandler {
              state = EditingSensor(state.modificationType)
            }
          )
        )
      )
      is EditingSensor -> DualLayer(baseRendering)
    }
  }

  override fun snapshotState(state: State) = state.toSnapshot()

  private fun Sensor.toolbarProps(context: RenderContext): ToolbarProps {
    val displayableSensorName = SensorUtil.getHumanStringType(this)
    return ToolbarProps(
      title = displayableSensorName.nullIfBlank() ?: name,
      subtitle = name.takeUnless { displayableSensorName.isNullOrBlank() },
      navigationIcon = MENU,
      onNavigationIconClicked = context.eventHandler {
        if (state !is EditingSensor) {
          state = EditingSensor(state.modificationType)
        } else {
          setOutput(BackPressed)
        }
      },
      overflowMenu = listOf(
        ToolbarAction(
          R.drawable.ic_restore_white_24dp,
          androidContext.getString(R.string.menu_reset),
          onClick = context.eventHandler {
            state = EditingSensor(props.modificationType)
          }
        ),
        ToolbarAction(
          R.drawable.ic_info_outline_white_24dp,
          androidContext.getString(R.string.menu_info),
          onClick = context.eventHandler {
            state = ShowingInfo(state.modificationType)
          }
        ),
      )
    )
  }

  private fun generateRows(
    renderState: State,
    renderProps: Props,
    context: RenderContext
  ): List<Row> {
    val defaultRows = listOf(
      Row.ModificationTypeRow(
        androidContext.resources.getString(R.string.do_nothing_radio_button),
        isSelected = renderState.modificationType is ModificationType.DoNothing,
        onClick = context.eventHandler {
          state = EditingSensor(ModificationType.DoNothing)
        }
      ),
      Row.ModificationTypeRow(
        androidContext.resources.getString(R.string.remove_sensor_radio_button),
        isSelected = renderState.modificationType is ModificationType.Remove,
        onClick = context.eventHandler {
          state = EditingSensor(ModificationType.Remove)
        }
      )
    )
    val mockSensorRow = Row.ModificationTypeRow(
      androidContext.resources.getString(R.string.mock_sensor_radio_button),
      isSelected = renderState.modificationType is ModificationType.Mock,
      onClick = context.eventHandler {
        if (state.modificationType !is ModificationType.Mock) {
          state =
            EditingSensor(modificationType = ModificationType.Mock(props.defaultMockableValues))
        }
      }
    )

    val isMockableSensor = renderProps.defaultMockableValues.isNotEmpty()

    return when {
      renderState.modificationType is ModificationType.Mock -> {
        val mockableRows = renderState.getMockableValues(renderProps).mapIndexed { index, value ->
          Row.MockableValueRow(
            mockableValue = value,
            onMockValueChanged = context.eventHandler { newValue ->
              state = EditingSensor(
                ModificationType.Mock(
                  mockedValues = state.getMockableValues(props).replace(index, newValue)
                )
              )
            })
        }
        defaultRows + mockSensorRow + mockableRows
      }
      isMockableSensor -> defaultRows + mockSensorRow
      else -> defaultRows
    }
  }
}

/**
 * Return either the mockable values stored in state (when the user has selected "Mock Values" as
 * a modification option) or the default mockable values if they don't have "Mock Values" selected.
 */
private fun State.getMockableValues(props: Props): List<MockableValue> {
  return if (modificationType is ModificationType.Mock) {
    (modificationType as ModificationType.Mock).mockedValues
  } else {
    props.defaultMockableValues
  }
}

fun <T> List<T>.replace(
  index: Int,
  replacement: T
): List<T> = let {
  it.toMutableList()
    .apply {
      set(index, replacement)
    }
}

private fun String?.nullIfBlank(): String? = if (isNullOrBlank()) null else this