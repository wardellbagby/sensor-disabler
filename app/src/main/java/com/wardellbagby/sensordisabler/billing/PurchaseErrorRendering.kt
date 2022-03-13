package com.wardellbagby.sensordisabler.billing

import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.squareup.workflow1.ui.*
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.views.dp


data class PurchaseErrorRendering(
  val onClose: () -> Unit
) : AndroidViewRendering<PurchaseErrorRendering> {
  override val viewFactory = BuilderViewFactory(
    type = PurchaseErrorRendering::class
  ) { initialRendering, initialViewEnvironment, contextForNewView, _ ->
    val close = MaterialButton(contextForNewView).apply {
      text = context.getString(R.string.close)
    }
    LinearLayout(contextForNewView).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(16.dp)

      addView(TextView(context).apply {
        text = context.getText(R.string.try_again_later)
        updatePadding(bottom = 16.dp)
      })
      addView(close)

      fun update(rendering: PurchaseErrorRendering, viewEnvironment: ViewEnvironment) {
        close.setOnClickListener {
          rendering.onClose()
        }
        backPressedHandler = rendering.onClose
      }

      bindShowRendering(initialRendering, initialViewEnvironment, ::update)
    }
  }
}