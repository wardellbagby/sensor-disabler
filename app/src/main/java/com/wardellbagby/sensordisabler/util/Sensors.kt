package com.wardellbagby.sensordisabler.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Parcelable
import com.wardellbagby.sensordisabler.sensordetail.MockableValue
import com.wardellbagby.sensordisabler.util.ModificationType.*
import kotlinx.parcelize.Parcelize

fun Sensor.getMockedValues(context: Context): List<MockableValue> {
  val prefs = context.getSensorPreferences()

  val mockedValuesKey = SensorUtil.generateUniqueSensorMockValuesKey(this)
  val mockedValues = prefs.getSensorMockedValues(mockedValuesKey)
  return if (mockedValues == null || mockedValues.isEmpty()) {
    defaultMockedValues(context)
  } else {
    mockedValues
      .zip(SensorUtil.getLabelsForSensor(context, this))
      .mapIndexed { index, (value, label) ->
        MockableValue(
          id = index,
          title = label,
          value = value,
          minimum = SensorUtil.getMinimumValueForSensor(this),
          maximum = maximumRange
        )
      }
  }
}

fun Sensor.defaultMockedValues(context: Context): List<MockableValue> {
  return SensorUtil.getLabelsForSensor(context, this)
    .mapIndexed { index, label ->
      MockableValue(
        id = index,
        title = label,
        value = SensorUtil.getMinimumValueForSensor(this),
        maximum = maximumRange,
        minimum = SensorUtil.getMinimumValueForSensor(this)
      )
    }
}

sealed class ModificationType : Parcelable {
  @Parcelize
  object DoNothing : ModificationType()

  @Parcelize
  object Remove : ModificationType()

  @Parcelize
  data class Mock(val mockedValues: List<MockableValue>) : ModificationType()
}

fun Sensor.getModificationType(context: Context): ModificationType {
  val prefs = context.getSensorPreferences()
  val enabledStatusKey = SensorUtil.generateUniqueSensorKey(this)
  return when (prefs.getInt(enabledStatusKey, Constants.SENSOR_STATUS_DO_NOTHING)) {
    Constants.SENSOR_STATUS_DO_NOTHING -> DoNothing
    Constants.SENSOR_STATUS_REMOVE_SENSOR -> Remove
    Constants.SENSOR_STATUS_MOCK_VALUES -> Mock(getMockedValues(context))
    else -> DoNothing
  }
}

fun Sensor.isEnabledForAppAndFilterType(
  context: Context,
  packageName: String,
  filterType: FilterType
): Boolean {
  val prefs = context.getSensorPreferences()

  val key = SensorUtil.generateUniqueSensorPackageBasedKey(this, packageName, filterType)
  return prefs.getBoolean(key, false)
}

fun Sensor.setEnabledForAppAndFilterType(
  isEnabled: Boolean,
  context: Context,
  packageName: String,
  filterType: FilterType
) {
  val prefs = context.getSensorPreferences()

  val key = SensorUtil.generateUniqueSensorPackageBasedKey(this, packageName, filterType)
  prefs.edit().putBoolean(key, isEnabled).apply()
}

fun Sensor.saveSettings(
  context: Context,
  modificationType: ModificationType
) {
  val enabledStatusKey = SensorUtil.generateUniqueSensorKey(this)
  val mockedValuesPrefsKey = SensorUtil.generateUniqueSensorMockValuesKey(this)

  val prefs = context.getSensorPreferences()

  prefs.putLegacySensorModificationType(enabledStatusKey, modificationType)

  if (modificationType is Mock) {
    prefs.putSensorMockedValues(
      mockedValuesPrefsKey,
      modificationType.mockedValues.map { it.value }.toFloatArray()
    )
  }
}

val Sensor.displayName: String
  get() = SensorUtil.getHumanStringType(this) ?: name

val Context.sensors: List<Sensor>
  get() = (getSystemService(Context.SENSOR_SERVICE) as SensorManager).getSensorList(Sensor.TYPE_ALL)