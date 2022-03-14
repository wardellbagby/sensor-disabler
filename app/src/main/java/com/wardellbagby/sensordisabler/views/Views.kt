package com.wardellbagby.sensordisabler.views

import android.view.View
import android.widget.TableLayout
import android.widget.TableRow

fun TableLayout.addRow(vararg children: View) {
  addView(TableRow(context).apply {
    children.forEach(::addView)
  })
}