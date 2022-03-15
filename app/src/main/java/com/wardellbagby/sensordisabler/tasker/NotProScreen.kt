package com.wardellbagby.sensordisabler.tasker

import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.squareup.workflow1.ui.*
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.views.dp

data class NotProScreen(
  val onBack: () -> Unit
) : AndroidViewRendering<NotProScreen> {
  override val viewFactory = BuilderViewFactory(
    type = NotProScreen::class
  ) { initialRendering, initialViewEnvironment, contextForNewView, _ ->
    val close = MaterialButton(contextForNewView).apply {
      text = context.getString(R.string.close)
    }
    LinearLayout(contextForNewView).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER
      setPadding(48.dp)

      addView(TextView(context).apply {
        text = context.getString(R.string.not_pro)
        updatePadding(bottom = 16.dp)
      })
      addView(close)

      fun update(rendering: NotProScreen, environment: ViewEnvironment) {
        backPressedHandler = rendering.onBack
        close.setOnClickListener { rendering.onBack() }
      }

      bindShowRendering(initialRendering, initialViewEnvironment, ::update)
    }
  }

}