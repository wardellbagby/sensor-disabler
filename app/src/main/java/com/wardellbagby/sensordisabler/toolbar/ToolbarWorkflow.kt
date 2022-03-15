package com.wardellbagby.sensordisabler.toolbar

import android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.squareup.workflow1.StatelessWorkflow
import com.squareup.workflow1.ui.LayoutRunner
import com.squareup.workflow1.ui.LayoutRunner.Companion.bind
import com.squareup.workflow1.ui.ViewEnvironment
import com.squareup.workflow1.ui.ViewFactory
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.toolbar.NavigationIcon.BACK
import com.wardellbagby.sensordisabler.toolbar.NavigationIcon.MENU

enum class NavigationIcon {
  MENU,
  BACK
}

data class ToolbarProps(
  val title: CharSequence,
  val subtitle: CharSequence? = null,
  val navigationIcon: NavigationIcon,
  val onNavigationIconClicked: () -> Unit,
  val overflowMenu: List<ToolbarAction> = listOf()
)

data class ToolbarAction(
  @DrawableRes val drawable: Int = 0,
  val title: CharSequence,
  val onClick: () -> Unit
)

data class ToolbarRendering(
  val title: CharSequence,
  val subtitle: CharSequence?,
  val navigationIcon: NavigationIcon,
  val overflowMenu: List<ToolbarAction>,
  val onIconClicked: () -> Unit
)

class ToolbarLayoutRunner(view: View) : LayoutRunner<ToolbarRendering> {
  private val toolbar = view as Toolbar
  override fun showRendering(
    rendering: ToolbarRendering,
    viewEnvironment: ViewEnvironment
  ) {
    toolbar.title = rendering.title
    toolbar.subtitle = rendering.subtitle
    toolbar.navigationIcon = when (rendering.navigationIcon) {
      MENU -> ContextCompat.getDrawable(toolbar.context, R.drawable.ic_menu)
      BACK -> ContextCompat.getDrawable(toolbar.context, R.drawable.ic_back)
    }

    toolbar.setNavigationOnClickListener { rendering.onIconClicked() }

    toolbar.menu.apply {
      clear()

      rendering.overflowMenu.forEach {
        val item = add(it.title)
        if (it.drawable != 0) {
          item.icon = ContextCompat.getDrawable(toolbar.context, it.drawable)
        }
        item.setOnMenuItemClickListener { _ ->
          it.onClick()
          return@setOnMenuItemClickListener true
        }
        item.setShowAsAction(if (it.drawable != 0) SHOW_AS_ACTION_IF_ROOM else SHOW_AS_ACTION_NEVER)
      }
    }
  }

  companion object : ViewFactory<ToolbarRendering> by bind(
    R.layout.toolbar_layout,
    ::ToolbarLayoutRunner
  )
}

object ToolbarWorkflow : StatelessWorkflow<ToolbarProps, Nothing, ToolbarRendering>() {
  override fun render(
    renderProps: ToolbarProps,
    context: RenderContext
  ): ToolbarRendering {
    return ToolbarRendering(
      renderProps.title,
      renderProps.subtitle,
      renderProps.navigationIcon,
      overflowMenu = renderProps.overflowMenu,
      onIconClicked = renderProps.onNavigationIconClicked
    )
  }
}