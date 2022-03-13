package com.wardellbagby.sensordisabler

import android.content.Context
import android.hardware.Sensor
import android.os.Parcelable
import com.squareup.workflow1.*
import com.squareup.workflow1.WorkflowAction.Companion.noAction
import com.squareup.workflow1.ui.ViewRegistry
import com.squareup.workflow1.ui.modal.AlertContainer
import com.squareup.workflow1.ui.toParcelable
import com.squareup.workflow1.ui.toSnapshot
import com.wardellbagby.sensordisabler.MainWorkflow.Props
import com.wardellbagby.sensordisabler.MainWorkflow.State
import com.wardellbagby.sensordisabler.MainWorkflow.State.*
import com.wardellbagby.sensordisabler.billing.BillingClientHelper
import com.wardellbagby.sensordisabler.billing.BillingClientHelper.Event.*
import com.wardellbagby.sensordisabler.billing.BillingModalWorkflow
import com.wardellbagby.sensordisabler.modals.DualLayer
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailLayoutRunner
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow.Output.BackPressed
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow.Output.Saved
import com.wardellbagby.sensordisabler.sensorlist.SensorListLayoutRunner
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow.Output.SelectedSensor
import com.wardellbagby.sensordisabler.settings.Output.Closed
import com.wardellbagby.sensordisabler.settings.SettingsLayoutRunner
import com.wardellbagby.sensordisabler.settings.SettingsWorkflow
import com.wardellbagby.sensordisabler.toolbar.ToolbarAction
import com.wardellbagby.sensordisabler.toolbar.ToolbarLayoutRunner
import com.wardellbagby.sensordisabler.toolbar.ToolbarProps
import com.wardellbagby.sensordisabler.toolbar.ToolbarWorkflow
import com.wardellbagby.sensordisabler.util.Constants
import com.wardellbagby.sensordisabler.util.getMockedValues
import com.wardellbagby.sensordisabler.util.getModificationType
import com.wardellbagby.sensordisabler.util.saveSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import com.wardellbagby.sensordisabler.sensordetail.SensorDetailWorkflow.Props as DetailProps
import com.wardellbagby.sensordisabler.sensorlist.SensorListWorkflow.Props as ListProps

val MainViewRegistry = ViewRegistry(
  ToolbarLayoutRunner,
  SensorDetailLayoutRunner,
  DrawerLayoutRunner,
  SensorListLayoutRunner,
  AlertContainer,
  SettingsLayoutRunner
)

@ActivityRetainedScoped
class MainWorkflow
@Inject constructor(
  @ApplicationContext private val androidContext: Context,
  private val sensorListWorkflow: SensorListWorkflow,
  private val sensorDetailWorkflow: SensorDetailWorkflow,
  private val settingsWorkflow: SettingsWorkflow,
  private val billingModalWorkflow: BillingModalWorkflow,
  private val billingClientHelper: BillingClientHelper,
  private val toaster: Toaster
) : StatefulWorkflow<Props, State, Nothing, DualLayer<*>>() {
  private companion object {
    private val CONSUMABLE_SKUS = listOf(
      Constants.SKU_DONATION_1,
      Constants.SKU_DONATION_5,
      Constants.SKU_DONATION_10
    )
  }

  data class Props(val sensors: List<Sensor>)

  @Parcelize
  data class UnacknowledgedPurchase(val sku: String, val purchaseToken: String) : Parcelable

  sealed class State : Parcelable {
    abstract val sensorIndex: Int
    abstract val unacknowledgedPurchases: List<UnacknowledgedPurchase>

    @Parcelize
    data class SensorList(
      override val sensorIndex: Int,
      override val unacknowledgedPurchases: List<UnacknowledgedPurchase> = listOf()
    ) : State()

    @Parcelize
    data class SensorDetails(
      override val sensorIndex: Int,
      override val unacknowledgedPurchases: List<UnacknowledgedPurchase> = listOf()
    ) : State()

    @Parcelize
    data class AppSettings(
      override val sensorIndex: Int,
      override val unacknowledgedPurchases: List<UnacknowledgedPurchase> = listOf()
    ) : State()

    fun withUnacknowledgedPurchases(
      unacknowledgedPurchases: List<UnacknowledgedPurchase>
    ): State {
      return when (this) {
        is AppSettings -> copy(unacknowledgedPurchases = unacknowledgedPurchases)
        is SensorDetails -> copy(unacknowledgedPurchases = unacknowledgedPurchases)
        is SensorList -> copy(unacknowledgedPurchases = unacknowledgedPurchases)
      }
    }
  }

  override fun initialState(
    props: Props,
    snapshot: Snapshot?
  ): State = snapshot?.toParcelable() ?: SensorList(sensorIndex = 0)

  override fun render(
    renderProps: Props,
    renderState: State,
    context: RenderContext
  ): DualLayer<*> {
    context.runningSideEffect("pending_purchase") {
      billingClientHelper.loadPendingPurchases()
    }
    context.runningWorker(worker = billingClientHelper.asWorker()) { event ->
      when (event) {
        Connected, Disconnected, Error -> noAction()
        is PurchasesUpdated -> action {
          when {
            event.completed.isNotEmpty() -> {
              val unacknowledgedPurchases = event.completed
                .filter { !it.isAcknowledged }
                .map {
                  UnacknowledgedPurchase(
                    sku = it.skus.first(),
                    purchaseToken = it.purchaseToken
                  )
                }
              if (unacknowledgedPurchases.isNotEmpty()) {
                toaster.showToast(androidContext.resources.getString(R.string.purchase_successful))
              }
              state = state.withUnacknowledgedPurchases(
                unacknowledgedPurchases = unacknowledgedPurchases
              )
            }
            event.pending.isNotEmpty() -> {
              toaster.showToast(androidContext.resources.getString(R.string.purchase_pending))
            }
          }
        }
      }
    }

    context.runningSideEffect(renderState.unacknowledgedPurchases.joinToString()) {
      for (purchase in renderState.unacknowledgedPurchases) {
        billingClientHelper.acknowledgePurchase(
          purchaseToken = purchase.purchaseToken,
          CONSUMABLE_SKUS.contains(purchase.sku)
        )
      }
    }

    val billingModalRendering = context.renderChild(billingModalWorkflow)
    val childRendering = when (renderState) {
      is SensorList, is SensorDetails -> {
        val drawerRendering =
          context.renderChild(
            sensorListWorkflow,
            props = ListProps(renderProps.sensors),
            handler = {
              when (it) {
                is SelectedSensor -> action {
                  state = SensorDetails(it.sensorIndex)
                }
                is SensorListWorkflow.Output.BackPressed -> action {
                  state = SensorDetails(state.sensorIndex)
                }
              }
            })

        val sensor = renderProps.sensors[renderState.sensorIndex]
        val contentRendering = context.renderChild(
          sensorDetailWorkflow,
          props = DetailProps(
            sensor = sensor,
            modificationType = sensor.getModificationType(androidContext),
            defaultMockableValues = sensor.getMockedValues(androidContext)
          ),
          handler = {
            when (it) {
              is Saved -> action {
                renderProps.sensors[renderState.sensorIndex].saveSettings(
                  androidContext, it.modificationType
                )
                toaster.showToast(androidContext.getString(R.string.settings_saved))
              }
              is BackPressed -> action {
                state = SensorList(state.sensorIndex)
              }
            }
          }
        )

        DrawerLayoutRendering(
          drawerRendering = drawerRendering,
          contentRendering = OptionalToolbarScreen(
            toolbar = context.renderChild(
              ToolbarWorkflow,
              props = contentRendering.beneathModals.toolbarProps.addSettings(context)
            ),
            content = contentRendering.beneathModals
          ),
          isDrawerOpened = renderState is SensorList,
          onDrawerClosed = context.eventHandler {
            state = SensorDetails(state.sensorIndex)
          }
        ).let { DualLayer(it) }
      }
      is AppSettings -> context.renderChild(
        settingsWorkflow,
        props = SettingsWorkflow.Props(renderProps.sensors),
        handler = {
          when (it) {
            Closed -> action { state = SensorDetails(state.sensorIndex) }
          }
        })
    }

    return DualLayer(
      base = childRendering.beneathModals,
      modal = billingModalRendering ?: childRendering.modals.firstOrNull()
    )
  }

  override fun snapshotState(state: State): Snapshot {
    return state.toSnapshot()
  }

  private fun ToolbarProps.addSettings(context: RenderContext): ToolbarProps {
    val settings = ToolbarAction(
      R.drawable.ic_settings,
      androidContext.getString(R.string.menu_settings),
      onClick = context.eventHandler {
        state = AppSettings(state.sensorIndex)
      }
    )

    return copy(overflowMenu = overflowMenu + settings)
  }
}