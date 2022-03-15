package com.wardellbagby.sensordisabler.billing

import android.content.Context
import android.os.Parcelable
import com.squareup.workflow1.Snapshot
import com.squareup.workflow1.StatefulWorkflow
import com.squareup.workflow1.action
import com.squareup.workflow1.runningWorker
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.Toaster
import com.wardellbagby.sensordisabler.billing.BillingClientHelper.Event.*
import com.wardellbagby.sensordisabler.billing.BillingWorkflow.Props
import com.wardellbagby.sensordisabler.billing.BillingWorkflow.State
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Handles connecting to Google Play's Billing API, acknowledging, and consuming purchases.
 *
 * This should always be rendered so long as the app is alive.
 */
class BillingWorkflow
@Inject constructor(
  @ApplicationContext private val androidContext: Context,
  private val billingClientHelper: BillingClientHelper,
  private val toaster: Toaster
) : StatefulWorkflow<Props, State, Nothing, PurchaseErrorRendering?>() {
  @Parcelize
  data class UnacknowledgedPurchase(val sku: String, val purchaseToken: String) : Parcelable
  data class Props(
    val consumableSkus: List<String>
  )

  data class State(
    val lastEvent: BillingClientHelper.Event? = null,
    val unacknowledgedPurchases: List<UnacknowledgedPurchase> = listOf()
  )

  override fun initialState(props: Props, snapshot: Snapshot?): State = State()

  override fun render(
    renderProps: Props,
    renderState: State,
    context: RenderContext
  ): PurchaseErrorRendering? {
    context.runningSideEffect("pending_purchase") {
      billingClientHelper.loadPendingPurchases()
    }
    context.runningWorker(worker = billingClientHelper.asWorker()) { event ->
      when (event) {
        Connected, Disconnected, Error -> action {
          state = state.copy(lastEvent = event)
        }
        is PurchasesUpdated -> {
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
              action {
                state = state.copy(
                  lastEvent = event, unacknowledgedPurchases = unacknowledgedPurchases
                )
              }
            }
            event.pending.isNotEmpty() -> action {
              toaster.showToast(androidContext.resources.getString(R.string.purchase_pending))
              state = state.copy(lastEvent = event)
            }
            else -> action { state = state.copy(lastEvent = event) }
          }
        }
      }
    }

    context.runningSideEffect(renderState.unacknowledgedPurchases.joinToString()) {
      for (purchase in renderState.unacknowledgedPurchases) {
        billingClientHelper.acknowledgePurchase(
          purchaseToken = purchase.purchaseToken,
          consume = renderProps.consumableSkus.contains(purchase.sku)
        )
      }
    }

    return when (renderState.lastEvent) {
      Error -> PurchaseErrorRendering(
        onClose = context.eventHandler {
          state = State(lastEvent = null)
        }
      )
      null, Connected, Disconnected, is PurchasesUpdated -> null
    }
  }

  override fun snapshotState(state: State): Snapshot? = null
}