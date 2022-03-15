package com.wardellbagby.sensordisabler.settings.filtering.settings

import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.use
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import coil.request.ImageRequest
import com.google.android.material.textview.MaterialTextView
import com.squareup.cycler.Recycler
import com.squareup.cycler.toDataSource
import com.squareup.workflow1.ui.AndroidViewRendering
import com.squareup.workflow1.ui.BuilderViewFactory
import com.squareup.workflow1.ui.backPressedHandler
import com.squareup.workflow1.ui.bindShowRendering
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.images.AppIconRequest
import com.wardellbagby.sensordisabler.images.CoilImageLoader
import com.wardellbagby.sensordisabler.views.dp

data class ApplicationUiData(
  val name: CharSequence,
  val packageName: String,
  val onClick: () -> Unit
)

data class PackageListScreen(
  val applications: List<ApplicationUiData>,
  val onBack: () -> Unit,
) : AndroidViewRendering<PackageListScreen> {
  override val viewFactory = BuilderViewFactory(
    type = PackageListScreen::class
  ) { initialRendering, initialViewEnvironment, contextForNewView, _ ->
    val recycler =
      Recycler.create<ApplicationUiData>(
        contextForNewView,
        layoutProvider = ::LinearLayoutManager
      ) {
        row<ApplicationUiData, View> {
          create { context ->
            val label = MaterialTextView(context).apply {
              updatePadding(left = 16.dp, top = 24.dp, right = 16.dp, bottom = 24.dp)

              context.obtainStyledAttributes(intArrayOf(R.attr.textAppearanceHeadline6)).use {
                val resId = it.getResourceId(0, 0)
                TextViewCompat.setTextAppearance(this, resId)
              }
            }
            val icon = ImageView(context).apply {
              layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp).apply {
                gravity = Gravity.CENTER_VERTICAL
              }
            }
            view = LinearLayout(contextForNewView).apply {
              orientation = LinearLayout.HORIZONTAL
              addView(icon)
              addView(label)
            }
            bind { data ->
              label.text = data.name
              initialViewEnvironment[CoilImageLoader].enqueue(
                ImageRequest.Builder(context)
                  .data(AppIconRequest(data.packageName))
                  .target(icon)
                  .build()
              )
              view.setOnClickListener { data.onClick() }
            }
          }
        }
      }

    fun update(rendering: PackageListScreen) {
      recycler.view.backPressedHandler = rendering.onBack
      recycler.data = rendering.applications.toDataSource()
    }

    recycler.view.bindShowRendering(initialRendering, initialViewEnvironment)
    { rendering, _ ->
      update(rendering)
    }

    return@BuilderViewFactory recycler.view
  }
}