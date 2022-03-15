package com.wardellbagby.sensordisabler

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.ImageLoader
import com.squareup.workflow1.ui.*
import com.wardellbagby.sensordisabler.images.CoilImageLoader
import com.wardellbagby.sensordisabler.images.appIconFetcherFactory
import com.wardellbagby.sensordisabler.modals.RenderingModalContainer
import com.wardellbagby.sensordisabler.util.sensors
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

private val viewRegistry = MainViewRegistry + RenderingModalContainer

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
  @Inject
  lateinit var activityProvider: ActivityProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    lifecycle.addObserver(activityProvider)

    val imageLoader = ImageLoader.Builder(context = applicationContext)
      .components {
        add(appIconFetcherFactory)
      }
      .build()
    val viewEnvironment = ViewEnvironment(
      mapOf(
        ViewRegistry to viewRegistry,
        CoilImageLoader to imageLoader
      )
    )
    Coil.setImageLoader(imageLoader)

    val model: AppViewModel by viewModels()
    setContentView(WorkflowLayout(this).apply {
      start(lifecycle, model.renderings, viewEnvironment)
    })
  }
}

@HiltViewModel
class AppViewModel
@Inject constructor(
  savedState: SavedStateHandle,
  workflow: MainWorkflow,
  @ApplicationContext context: Context
) : ViewModel() {
  val renderings: StateFlow<Any> =
    renderWorkflowIn(
      workflow = workflow,
      scope = viewModelScope,
      prop = MainWorkflow.Props(context.sensors),
      savedStateHandle = savedState
    )
}