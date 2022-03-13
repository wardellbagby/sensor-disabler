package com.wardellbagby.sensordisabler

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import com.squareup.workflow1.ui.LayoutRunner
import com.squareup.workflow1.ui.LayoutRunner.Companion.bind
import com.squareup.workflow1.ui.ViewEnvironment
import com.squareup.workflow1.ui.ViewFactory
import com.squareup.workflow1.ui.WorkflowViewStub

data class DrawerLayoutRendering(
  val drawerRendering: Any,
  val contentRendering: Any,
  val isDrawerOpened: Boolean,
  val onDrawerClosed: () -> Unit
)

class DrawerLayoutRunner(view: View) : LayoutRunner<DrawerLayoutRendering> {
  private val drawerLayout = view as DrawerLayout
  private val drawerStub: WorkflowViewStub = drawerLayout.findViewById(R.id.drawer_stub)
  private val contentStub: WorkflowViewStub = drawerLayout.findViewById(R.id.content_stub)

  private var lastDrawerListener: DrawerListener? = null

  @SuppressLint("RtlHardcoded")
  override fun showRendering(
    rendering: DrawerLayoutRendering,
    viewEnvironment: ViewEnvironment
  ) {
    lastDrawerListener?.let { drawerLayout.removeDrawerListener(it) }

    drawerStub.update(rendering.drawerRendering, viewEnvironment)
    contentStub.update(rendering.contentRendering, viewEnvironment)

    when {
      rendering.isDrawerOpened && !drawerLayout.isDrawerOpen(Gravity.LEFT) ->
        drawerLayout.openDrawer(Gravity.LEFT)
      !rendering.isDrawerOpened && drawerLayout.isDrawerOpen(Gravity.LEFT) ->
        drawerLayout.closeDrawer(Gravity.LEFT)
    }

    lastDrawerListener = object : DrawerListener {
      override fun onDrawerStateChanged(newState: Int) = Unit
      override fun onDrawerSlide(
        drawerView: View,
        slideOffset: Float
      ) = Unit

      override fun onDrawerClosed(drawerView: View) = rendering.onDrawerClosed()
      override fun onDrawerOpened(drawerView: View) = Unit
    }.also { drawerLayout.addDrawerListener(it) }
  }

  companion object : ViewFactory<DrawerLayoutRendering> by bind(
    R.layout.drawer_layout,
    ::DrawerLayoutRunner
  )
}