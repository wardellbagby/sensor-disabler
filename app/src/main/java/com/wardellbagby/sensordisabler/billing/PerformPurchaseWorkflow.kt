package com.wardellbagby.sensordisabler.billing

import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.squareup.workflow1.*
import com.wardellbagby.sensordisabler.ActivityProvider
import com.wardellbagby.sensordisabler.billing.BillingClientOutput.*
import com.wardellbagby.sensordisabler.billing.PerformPurchaseWorkflow.*
import com.wardellbagby.sensordisabler.billing.PerformPurchaseWorkflow.State.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PerformPurchaseWorkflow
@Inject constructor(
  @ApplicationContext private val androidContext: Context,
  private val activityProvider: ActivityProvider
) : StatefulWorkflow<Props, State, Output, Unit>() {
  data class Props(val sku: String, val shouldConsume: Boolean)
  data class Output(val success: Boolean)
  sealed class State {
    abstract val billingClient: BillingClient

    object LoadingBillingClient : State() {
      override val billingClient: BillingClient
        get() = error("BillingClient hasn't been initialized!")
    }

    data class PerformingPurchase(override val billingClient: BillingClient) : State()
    data class AcknowledgingPurchase(
      val purchaseToken: String,
      override val billingClient: BillingClient
    ) : State()
  }

  override fun initialState(props: Props, snapshot: Snapshot?): State {
    return LoadingBillingClient
  }

  override fun render(renderProps: Props, renderState: State, context: RenderContext) {
    context.runningWorker(
      BillingClientSetupWorker(androidContext),
      handler = { output ->
        when (output) {
          is BillingClientConnected -> action {
            state = PerformingPurchase(output.client)
          }
          is PurchasesUpdated -> action {
            if (output.result.responseCode != BillingResponseCode.OK || output.purchases.isNullOrEmpty()) {
              setOutput(Output(success = false))
            } else {
              state =
                AcknowledgingPurchase(
                  purchaseToken = output.purchases.first().purchaseToken,
                  billingClient = state.billingClient
                )
            }
          }
          BillingClientConnectionError, BillingClientDisconnected ->
            action {
              setOutput(Output(success = false))
            }
        }
      })
    when (renderState) {
      is LoadingBillingClient -> {
        // Purposefully left empty; we're always loading the BillingClient in every state, but
        // we have a specific state for when it's finally loaded.
      }
      is PerformingPurchase -> {
        val activity =
          activityProvider.activity ?: error("No activity can be used to launch the billing flow")
        context.runningWorker(
          PurchaseSkuWorker(
            renderState.billingClient,
            activity,
            sku = renderProps.sku
          ),
          handler = { success: Boolean ->
            action {
              if (!success) {
                setOutput(Output(success = false))
              }
            }
          })
      }
      is AcknowledgingPurchase -> {
        context.runningWorker(Worker.from {
          val result = renderState.billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
              .setPurchaseToken(renderState.purchaseToken)
              .build()
          )
          if (renderProps.shouldConsume) {
            renderState.billingClient.consumePurchase(
              ConsumeParams.newBuilder().setPurchaseToken(renderState.purchaseToken).build()
            )
          }

          result
        }) {
          action {
            setOutput(Output(success = it.responseCode == BillingResponseCode.OK))
          }
        }
      }
    }
  }

  override fun snapshotState(state: State): Snapshot? = null
}