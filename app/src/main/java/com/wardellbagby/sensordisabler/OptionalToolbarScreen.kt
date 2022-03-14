package com.wardellbagby.sensordisabler

import androidx.core.view.isVisible
import com.squareup.workflow1.ui.AndroidViewRendering
import com.squareup.workflow1.ui.LayoutRunner
import com.squareup.workflow1.ui.WorkflowViewStub
import com.wardellbagby.sensordisabler.toolbar.ToolbarRendering

data class OptionalToolbarScreen(
  val toolbar: ToolbarRendering? = null,
  val content: Any
) : AndroidViewRendering<OptionalToolbarScreen> {
  override val viewFactory =
    LayoutRunner.bind<OptionalToolbarScreen>(R.layout.optional_toolbar_layout) { view ->
      val toolbarStub = view.findViewById<WorkflowViewStub>(R.id.toolbar_stub)
      val contentStub = view.findViewById<WorkflowViewStub>(R.id.content_stub)
      LayoutRunner { rendering, viewEnvironment ->
        toolbarStub.isVisible = rendering.toolbar != null
        rendering.toolbar?.let { toolbarStub.update(it, viewEnvironment) }
        contentStub.update(rendering.content, viewEnvironment)
      }
    }

}