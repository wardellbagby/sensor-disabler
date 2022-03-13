package com.wardellbagby.sensordisabler.settings

import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.squareup.workflow1.ui.*
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.views.dp

data class FailedToPurchaseDialog(
  val onClose: () -> Unit
) : AndroidViewRendering<FailedToPurchaseDialog> {
//  override val viewFactory = composeViewFactory { rendering: FailedToPurchaseDialog, environment ->
//    LocalView.current.backPressedHandler = rendering.onClose
//    CompositionRoot(modifier = Modifier.wrapContentSize()) {
//      Column {
//        Text(text = stringResource(R.string.purchase_failed), style = MaterialTheme.typography.h5)
//        Text(text = stringResource(R.string.try_again_later))
//      }
//    }
//  }

  override val viewFactory = BuilderViewFactory(
    type = FailedToPurchaseDialog::class
  ) { initialRendering, initialViewEnvironment, contextForNewView, _ ->
    LinearLayout(contextForNewView).apply {
      val closeButton = MaterialButton(context).apply {
        setText(R.string.close)
      }

      orientation = LinearLayout.VERTICAL
      setPadding(4.dp)
      gravity = Gravity.CENTER
      fun update(rendering: FailedToPurchaseDialog, viewEnvironment: ViewEnvironment) {
        backPressedHandler = rendering.onClose
        closeButton.setOnClickListener { rendering.onClose() }
      }

      addView(TextView(context).apply {
        TextViewCompat.setTextAppearance(
          this,
          com.google.android.material.R.style.TextAppearance_MaterialComponents_Headline6
        )
        setText(R.string.purchase_failed)
      })
      addView(TextView(context).apply {
        updatePadding(top = 8.dp, bottom = 16.dp)
        setText(R.string.try_again_later)
      })
      addView(closeButton)

      bindShowRendering(initialRendering, initialViewEnvironment, ::update)
    }
  }
}