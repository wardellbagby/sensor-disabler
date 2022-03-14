package com.wardellbagby.sensordisabler.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.wardellbagby.sensordisabler.R

// TODO Make this use withCompositionRoot once https://github.com/square/workflow-kotlin/issues/698 is fixed
@Composable
fun CompositionRoot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  MaterialTheme(
    colors = if (isSystemInDarkTheme()) darkColors(
      primary = colorResource(id = R.color.primaryColor),
      primaryVariant = colorResource(id = R.color.primaryLightColor),
      secondary = colorResource(id = R.color.secondaryColor),
      secondaryVariant = colorResource(id = R.color.secondaryLightColor)
    ) else lightColors(
      primary = colorResource(id = R.color.primaryColor),
      primaryVariant = colorResource(id = R.color.primaryLightColor),
      secondary = colorResource(id = R.color.secondaryColor),
      secondaryVariant = colorResource(id = R.color.secondaryLightColor)
    )
  ) {
    Scaffold(modifier = modifier) {
      content()
    }
  }
}