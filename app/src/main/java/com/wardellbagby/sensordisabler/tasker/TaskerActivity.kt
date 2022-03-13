package com.wardellbagby.sensordisabler.tasker

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.squareup.workflow1.ui.ViewEnvironment
import com.squareup.workflow1.ui.ViewRegistry
import com.squareup.workflow1.ui.WorkflowLayout
import com.squareup.workflow1.ui.modal.AlertContainer
import com.squareup.workflow1.ui.renderWorkflowIn
import com.wardellbagby.sensordisabler.ActivityProvider
import com.wardellbagby.sensordisabler.sensordetail.MockableValue
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailLayoutRunner
import com.wardellbagby.sensordisabler.sensorlist.SensorListLayoutRunner
import com.wardellbagby.sensordisabler.tasker.TaskerWorkflow.Output.Cancelled
import com.wardellbagby.sensordisabler.tasker.TaskerWorkflow.Output.Saved
import com.wardellbagby.sensordisabler.util.ModificationType
import com.wardellbagby.sensordisabler.util.ModificationType.*
import com.wardellbagby.sensordisabler.util.SensorUtil
import com.wardellbagby.sensordisabler.util.displayName
import com.wardellbagby.sensordisabler.util.sensors
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

private val viewRegistry = ViewRegistry(
  SensorDetailLayoutRunner,
  SensorListLayoutRunner,
  AlertContainer,
)

private fun Saved.toIntent(): Intent {
  return Intent().apply {
    putExtra(extraBlurbKey, toBlurb())
    putExtra(extraBundleKey, Bundle().apply {
      sensor.persist(this)
      modificationType.persist(this)
    })
  }
}

private fun Saved.toBlurb(): String {
  val builder = StringBuilder(sensor.displayName)
  builder.append(" will be set to ")
  builder.append(
    when (modificationType) {
      DoNothing -> "do nothing."
      Remove -> "be removed"
      is Mock -> "have its values mocked. The values will be: ${
        modificationType.mockedValues.map { "${it.title} ${it.value}" }
      }"
    }
  )
  return builder.toString()
}

@AndroidEntryPoint
class TaskerActivity : AppCompatActivity() {
  @Inject
  lateinit var activityProvider: ActivityProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    lifecycle.addObserver(activityProvider)

    val viewEnvironment = ViewEnvironment(
      mapOf(
        ViewRegistry to viewRegistry,
      )
    )

    val model: TaskerViewModel by viewModels()

    setContentView(WorkflowLayout(this).apply {
      start(lifecycle, model.renderings, viewEnvironment)
    })

    lifecycleScope.launchWhenCreated {
      model.outputs
        .collect {
          when (it) {
            Cancelled -> setResult(RESULT_CANCELED)
            is Saved -> setResult(RESULT_OK, it.toIntent())
          }
          finish()
        }
    }
  }
}

@HiltViewModel
class TaskerViewModel
@Inject constructor(
  savedState: SavedStateHandle,
  workflow: TaskerWorkflow,
  @ApplicationContext context: Context
) : ViewModel() {
  private val _outputs = MutableSharedFlow<TaskerWorkflow.Output>()
  val outputs: Flow<TaskerWorkflow.Output> = _outputs
  val renderings: StateFlow<Any> =
    renderWorkflowIn(
      workflow = workflow,
      scope = viewModelScope,
      prop = TaskerWorkflow.Props(context.sensors),
      savedStateHandle = savedState
    ) {
      _outputs.emit(it)
    }
}