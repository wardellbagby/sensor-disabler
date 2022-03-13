package com.wardellbagby.sensordisabler.settings.filtering.settings

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.os.Parcelable
import com.squareup.workflow1.*
import com.squareup.workflow1.ui.toParcelable
import com.squareup.workflow1.ui.toSnapshot
import com.wardellbagby.sensordisabler.OptionalToolbarScreen
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.settings.filtering.settings.FilterSettingsWorkflow.*
import com.wardellbagby.sensordisabler.settings.filtering.settings.FilterSettingsWorkflow.Output.Closed
import com.wardellbagby.sensordisabler.settings.filtering.settings.FilterSettingsWorkflow.State.*
import com.wardellbagby.sensordisabler.toolbar.NavigationIcon
import com.wardellbagby.sensordisabler.toolbar.ToolbarProps
import com.wardellbagby.sensordisabler.toolbar.ToolbarWorkflow
import com.wardellbagby.sensordisabler.util.displayName
import com.wardellbagby.sensordisabler.util.getFilterType
import com.wardellbagby.sensordisabler.util.isEnabledForAppAndFilterType
import com.wardellbagby.sensordisabler.util.setEnabledForAppAndFilterType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class FilterSettingsWorkflow
@Inject constructor(
  @ApplicationContext private val androidContext: Context
) : StatefulWorkflow<Props, State, Output, OptionalToolbarScreen>() {
  @Parcelize
  data class ApplicationData(
    val packageName: String,
    val label: CharSequence,
  ) : Parcelable

  data class Props(
    val sensors: List<Sensor>
  )

  sealed class State : Parcelable {
    abstract val applications: List<ApplicationData>

    @Parcelize
    object LoadingPackages : State() {
      override val applications
        get() = error("No applications when loading packages")
    }

    @Parcelize
    data class SelectingPackage(
      override val applications: List<ApplicationData>
    ) : State()

    @Parcelize
    data class ViewingPackageSettings(
      override val applications: List<ApplicationData>,
      val selectedApplication: ApplicationData
    ) : State()
  }

  sealed class Output {
    object Closed : Output()
  }

  override fun initialState(props: Props, snapshot: Snapshot?): State {
    return snapshot?.toParcelable() ?: LoadingPackages
  }

  override fun render(
    renderProps: Props,
    renderState: State,
    context: RenderContext
  ): OptionalToolbarScreen {
    return when (renderState) {
      LoadingPackages -> {
        context.runningWorker(Worker.from {
          androidContext.packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)
            .asSequence()
            .filter {
              it.packageName != androidContext.packageName
            }
            .map {
              androidContext.packageManager.getApplicationInfo(it.packageName, 0)
            }
            .map {
              ApplicationData(
                packageName = it.packageName,
                label = it.loadLabel(androidContext.packageManager)
              )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.toString().lowercase() }
            .toList()
        }) {
          action { state = SelectingPackage(it) }
        }
        LoadingAppsScreen(
          onBack = context.eventHandler {
            setOutput(Closed)
          }
        ).withToolbar(context)
      }
      is SelectingPackage -> PackageListScreen(
        applications = renderState.applications.map {
          ApplicationUiData(
            name = it.label,
            packageName = it.packageName,
            onClick = context.eventHandler {
              state = ViewingPackageSettings(
                applications = state.applications,
                selectedApplication = it
              )
            }
          )
        },
        onBack = context.eventHandler {
          setOutput(Closed)
        }
      ).withToolbar(context)
      is ViewingPackageSettings -> ViewingApplicationSettingsScreen(
        filterType = androidContext.getFilterType(),
        applicationData = renderState.selectedApplication,
        sensors = renderProps.sensors.map {
          SelectableSensor(
            name = it.displayName,
            isChecked = it.isEnabledForAppAndFilterType(
              androidContext,
              renderState.selectedApplication.packageName,
              androidContext.getFilterType()
            ),
            onCheckedChanged = context.eventHandler { isChecked ->
              it.setEnabledForAppAndFilterType(
                isEnabled = isChecked,
                context = androidContext,
                filterType = androidContext.getFilterType(),
                packageName = renderState.selectedApplication.packageName
              )
            }
          )
        },
        onBack = context.eventHandler {
          state = SelectingPackage(state.applications)
        }).withToolbar(
        context,
        title = renderState.selectedApplication.label,
        onBack = context.eventHandler {
          state = SelectingPackage(state.applications)
        })
    }
  }

  override fun snapshotState(state: State): Snapshot {
    return state.toSnapshot()
  }

  private fun Any.withToolbar(
    context: RenderContext,
    title: CharSequence = androidContext.getString(R.string.filter_settings_title),
    onBack: () -> Unit = context.eventHandler {
      setOutput(Closed)
    }
  ): OptionalToolbarScreen {
    return OptionalToolbarScreen(
      toolbar = context.renderChild(
        ToolbarWorkflow,
        props = ToolbarProps(
          title = title,
          navigationIcon = NavigationIcon.BACK,
          onNavigationIconClicked = onBack
        )
      ),
      content = this
    )
  }
}