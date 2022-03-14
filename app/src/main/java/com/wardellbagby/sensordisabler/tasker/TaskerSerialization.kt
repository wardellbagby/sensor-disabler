package com.wardellbagby.sensordisabler.tasker

import android.content.Context
import android.hardware.Sensor
import android.os.Bundle
import com.wardellbagby.sensordisabler.sensordetail.MockableValue
import com.wardellbagby.sensordisabler.util.ModificationType
import com.wardellbagby.sensordisabler.util.SensorUtil

const val taskerFireSettings = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
const val extraBundleKey = "com.twofortyfouram.locale.intent.extra.BUNDLE"
const val extraBlurbKey = "com.twofortyfouram.locale.intent.extra.BLURB"

private const val sensorKey = "sensor"
private const val modificationTypeKey = "modification_type"
private const val mockLabelsKey = "mock_labels"
private const val mockValuesKey = "mock_values"

private const val nothingModificationTypeValue = "nothing"
private const val removeModificationTypeValue = "remove"
private const val mockModificationTypeValue = "mock"

fun Sensor.persist(bundle: Bundle) {
  bundle.putString(sensorKey, SensorUtil.generateUniqueSensorKey(this))
}

fun ModificationType.persist(bundle: Bundle) {
  when (this) {
    ModificationType.DoNothing -> bundle.putString(
      modificationTypeKey,
      nothingModificationTypeValue
    )
    ModificationType.Remove -> bundle.putString(modificationTypeKey, removeModificationTypeValue)
    is ModificationType.Mock -> {
      val labels = mockedValues.map { it.title }
      val values = mockedValues.map { it.value }
      bundle.putString(modificationTypeKey, mockModificationTypeValue)
      bundle.putStringArray(mockLabelsKey, labels.toTypedArray())
      bundle.putFloatArray(mockValuesKey, values.toFloatArray())
    }
  }
}

fun Bundle.getSensor(context: Context): Sensor? {
  val sensorKey = getString(sensorKey)
  return SensorUtil.getSensorFromUniqueSensorKey(context, sensorKey)
}

fun Bundle.getModificationType(sensor: Sensor): ModificationType? {
  return when (getString(modificationTypeKey)) {
    nothingModificationTypeValue -> ModificationType.DoNothing
    removeModificationTypeValue -> ModificationType.Remove
    mockModificationTypeValue -> {
      val labels = getStringArray(mockLabelsKey)
      val values = getFloatArray(mockValuesKey)?.toList()

      if (labels == null || values == null || labels.size != values.size) {
        null
      } else {
        ModificationType.Mock(
          labels.zip(values)
            .mapIndexed { index, (label, value) ->
              MockableValue(
                id = index,
                title = label,
                value = value,
                maximum = sensor.maximumRange,
                minimum = SensorUtil.getMinimumValueForSensor(sensor)
              )
            }
        )
      }
    }
    else -> null
  }
}