package com.wardellbagby.sensordisabler.xposed

import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.squareup.workflow1.StatelessWorkflow
import com.squareup.workflow1.ui.*
import com.wardellbagby.sensordisabler.ActivityProvider
import com.wardellbagby.sensordisabler.BuildConfig
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.views.dp
import javax.inject.Inject


object XposedAvailable {
  /**
   * Whether Xposed (or some alternative Xposed-compatible framework) is installed
   * and has loaded Sensor Disabler.
   *
   * The value for this function will be changed by the Xposed hook.
   */
  fun isXposedAvailable(): Boolean = false
}

data class XposedUnavailableScreen(
  val onUninstall: () -> Unit,
  val onExit: () -> Unit
) : AndroidViewRendering<XposedUnavailableScreen> {
  override val viewFactory = BuilderViewFactory(
    type = XposedUnavailableScreen::class,
    viewConstructor = { initialRendering, initialViewEnvironment, contextForNewView, _ ->
      val uninstall = MaterialButton(contextForNewView).apply {
        text = context.getString(R.string.uninstall)
      }
      val exit = MaterialButton(contextForNewView).apply {
        text = context.getString(R.string.exit)
      }
      LinearLayout(contextForNewView).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER

        addView(TextView(context).apply {
          text = context.getString(R.string.xposed_unavailable)
          updatePadding(bottom = 16.dp)
        })
        LinearLayout(context).apply {
          orientation = LinearLayout.HORIZONTAL
          addView(uninstall)
          addView(
            Space(context),
            LinearLayout.LayoutParams(0, 0).apply {
              weight = 1.0f
            })
          addView(exit)
        }.also { addView(it, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)) }

        fun update(rendering: XposedUnavailableScreen, environment: ViewEnvironment) {
          backPressedHandler = rendering.onUninstall
          uninstall.setOnClickListener { rendering.onUninstall() }
          exit.setOnClickListener { rendering.onExit() }
        }

        bindShowRendering(initialRendering, initialViewEnvironment, ::update)
      }
    }
  )

}

@Suppress("FunctionName")
class XposedUnavailableWorkflow
@Inject constructor(
  private val activityProvider: ActivityProvider
) : StatelessWorkflow<Unit, Nothing, XposedUnavailableScreen?>() {

  override fun render(
    renderProps: Unit,
    context: RenderContext
  ): XposedUnavailableScreen? {
    return if (!XposedAvailable.isXposedAvailable()) {
      XposedUnavailableScreen(
        onUninstall = context.eventHandler {
          val packageURI: Uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
          val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          activityProvider.activity?.startActivity(uninstallIntent)
        },
        onExit = context.eventHandler {
          activityProvider.activity?.finishAffinity()
        }
      )
    } else {
      null
    }
  }
}