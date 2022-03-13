package com.wardellbagby.sensordisabler.sensordetail

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class SensorModification(open val isSelected: Boolean) : Parcelable {
  @Parcelize
  data class DoNothing(override val isSelected: Boolean) : SensorModification(isSelected)

  @Parcelize
  data class Remove(override val isSelected: Boolean) : SensorModification(isSelected)

  @Parcelize
  data class MockValues(
    override val isSelected: Boolean,
    val mockableValues: List<MockableValue>
  ) : SensorModification(isSelected)

  fun with(isSelected: Boolean): SensorModification {
    return when (this) {
      is DoNothing -> copy(isSelected = isSelected)
      is Remove -> copy(isSelected = isSelected)
      is MockValues -> copy(isSelected = isSelected)
    }
  }
}

@Parcelize
data class MockableValue(
  val id: Int,
  val title: String,
  val minimum: Float,
  val maximum: Float,
  val value: Float
) : Parcelable