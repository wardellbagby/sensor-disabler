package com.wardellbagby.sensordisabler.tasker

import android.content.Context
import android.hardware.Sensor
import android.os.Parcelable
import com.squareup.workflow1.Snapshot
import com.squareup.workflow1.StatefulWorkflow
import com.squareup.workflow1.action
import com.squareup.workflow1.ui.toParcelable
import com.squareup.workflow1.ui.toSnapshot
import com.wardellbagby.sensordisabler.modals.DualLayer
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow.Output.Saved
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow.Output.SelectedSensor
import com.wardellbagby.sensordisabler.tasker.TaskerWorkflow.*
import com.wardellbagby.sensordisabler.tasker.TaskerWorkflow.State.ModifyingSensor
import com.wardellbagby.sensordisabler.tasker.TaskerWorkflow.State.PickingSensor
import com.wardellbagby.sensordisabler.util.ModificationType
import com.wardellbagby.sensordisabler.util.defaultMockedValues
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class TaskerWorkflow
@Inject constructor(
  private val sensorListWorkflow: SensorListWorkflow,
  private val sensorDetailWorkflow: SensorDetailWorkflow,
  @ApplicationContext private val androidContext: Context
) : StatefulWorkflow<Props, State, Output, DualLayer<*>>() {
  data class Props(val sensors: List<Sensor>, val isPro: Boolean)
  sealed class State : Parcelable {
    abstract val sensorIndex: Int

    @Parcelize
    data class PickingSensor(override val sensorIndex: Int) : State()

    @Parcelize
    data class ModifyingSensor(override val sensorIndex: Int) : State()
  }

  sealed class Output {
    data class Saved(val sensor: Sensor, val modificationType: ModificationType) : Output()
    object Cancelled : Output()
  }

  override fun initialState(props: Props, snapshot: Snapshot?) =
    snapshot?.toParcelable() ?: PickingSensor(sensorIndex = 0)

  override fun render(
    renderProps: Props,
    renderState: State,
    context: RenderContext
  ): DualLayer<*> {
    if (!renderProps.isPro) {
      return NotProScreen(
        onBack = context.eventHandler {
          setOutput(Output.Cancelled)
        }
      ).let { DualLayer(it) }
    }
    return when (renderState) {
      is PickingSensor -> context.renderChild(
        child = sensorListWorkflow,
        props = SensorListWorkflow.Props(renderProps.sensors)
      ) {
        when (it) {
          is SelectedSensor -> action { state = ModifyingSensor(it.sensorIndex) }
          SensorListWorkflow.Output.BackPressed -> action {
            setOutput(Output.Cancelled)
          }
        }
      }.let { DualLayer(it) }
      is ModifyingSensor -> {
        val sensor = renderProps.sensors[renderState.sensorIndex]

        context.renderChild(
          child = sensorDetailWorkflow,
          props = SensorDetailWorkflow.Props(
            sensor = sensor,
            modificationType = ModificationType.DoNothing,
            defaultMockableValues = sensor.defaultMockedValues(androidContext)
          )
        ) {
          when (it) {
            is Saved -> action {
              setOutput(Output.Saved(props.sensors[state.sensorIndex], it.modificationType))
            }
            SensorDetailWorkflow.Output.BackPressed -> action {
              state = PickingSensor(sensorIndex = state.sensorIndex)
            }
          }
        }
      }
    }
  }

  override fun snapshotState(state: State) = state.toSnapshot()
}