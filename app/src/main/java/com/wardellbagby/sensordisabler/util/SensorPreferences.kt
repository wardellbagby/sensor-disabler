package com.wardellbagby.sensordisabler.util

import android.content.Context
import android.content.SharedPreferences

private const val valuesSeparator = ":"

fun Context.getSensorPreferences(): SharedPreferences = getSharedPreferences(
  Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE
)

fun SharedPreferences.getSensorMockedValues(key: String): FloatArray? {
  return if (contains(key)) {
    val mockedValues = getString(key, null)
    if (mockedValues.isNullOrBlank()) {
      null
    } else {
      mockedValues.split(valuesSeparator)
        .filter { it.isNotBlank() }
        .map { it.toFloat() }
        .toFloatArray()
    }
  } else {
    floatArrayOf()
  }
}

fun SharedPreferences.putSensorMockedValues(key: String, values: FloatArray) {
  edit()
    .putString(key, values.joinToString(valuesSeparator))
    .apply()
}

private fun ModificationType.toLegacyConstant(): Int {
  return when (this) {
    ModificationType.DoNothing -> Constants.SENSOR_STATUS_DO_NOTHING
    ModificationType.Remove -> Constants.SENSOR_STATUS_REMOVE_SENSOR
    is ModificationType.Mock -> Constants.SENSOR_STATUS_MOCK_VALUES
  }
}

fun SharedPreferences.putLegacySensorModificationType(
  key: String,
  modificationType: ModificationType
) {
  edit()
    .putInt(key, modificationType.toLegacyConstant())
    .apply()
}

fun SharedPreferences.getLegacySensorModificationType(key: String): Int? {
  return when (getInt(key, -1)) {
    Constants.SENSOR_STATUS_DO_NOTHING -> Constants.SENSOR_STATUS_DO_NOTHING
    Constants.SENSOR_STATUS_REMOVE_SENSOR -> Constants.SENSOR_STATUS_REMOVE_SENSOR
    Constants.SENSOR_STATUS_MOCK_VALUES -> Constants.SENSOR_STATUS_MOCK_VALUES
    else -> null
  }
}