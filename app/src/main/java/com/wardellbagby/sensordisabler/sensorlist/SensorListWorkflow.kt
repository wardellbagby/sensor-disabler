package com.wardellbagby.sensordisabler.sensorlist

import android.hardware.Sensor
import com.squareup.workflow1.StatelessWorkflow
import com.squareup.workflow1.action
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow.Output
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow.Output.BackPressed
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow.Output.SelectedSensor
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow.Props
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow.Rendering.SensorData
import com.wardellbagby.sensordisabler.util.SensorUtil
import com.wardellbagby.sensordisabler.util.displayName
import javax.inject.Inject

class SensorListWorkflow
@Inject constructor() : StatelessWorkflow<Props, Output, SensorListWorkflow.Rendering>() {
  data class Rendering(
    val sensorData: List<SensorData>,
    val onBack: () -> Unit
  ) {
    data class SensorData(
      val title: String,
      val dangerous: Boolean,
      val onClick: () -> Unit
    )
  }

  data class Props(val sensors: List<Sensor>)
  sealed class Output {
    data class SelectedSensor(val sensorIndex: Int) : Output()
    object BackPressed : Output()
  }

  override fun render(
    renderProps: Props,
    context: RenderContext
  ): Rendering {
    val sink = context.actionSink

    return Rendering(
      onBack = context.eventHandler {
        setOutput(BackPressed)
      },
      sensorData = renderProps.sensors.mapIndexed { index, sensor ->
        SensorData(
          title = sensor.displayName,
          dangerous = SensorUtil.isDangerousSensor(sensor),
          onClick = { sink.send(action { setOutput(SelectedSensor(index)) }) },
        )
      }
    )
  }
}