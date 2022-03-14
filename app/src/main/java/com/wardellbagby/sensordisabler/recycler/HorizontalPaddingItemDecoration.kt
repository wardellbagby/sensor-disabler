package com.wardellbagby.sensordisabler.recycler

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State

class HorizontalPaddingItemDecoration(
  private val padding: Int
) : ItemDecoration() {
  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: State
  ) {
    with(outRect) {
      left = padding
      right = padding
    }
  }
}