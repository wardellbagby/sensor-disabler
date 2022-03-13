package com.wardellbagby.sensordisabler.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.squareup.workflow1.Snapshot
import com.squareup.workflow1.StatefulWorkflow
import com.squareup.workflow1.action
import com.squareup.workflow1.runningWorker
import com.wardellbagby.sensordisabler.billing.QuerySkuWorkflow.*
import com.wardellbagby.sensordisabler.billing.QuerySkuWorkflow.State.LoadingBillingClient
import com.wardellbagby.sensordisabler.billing.QuerySkuWorkflow.State.LoadingInAppPurchases
import com.wardellbagby.sensordisabler.settings.InAppPurchaseQueryOutput.ErrorQueryingPurchases
import com.wardellbagby.sensordisabler.settings.InAppPurchaseQueryOutput.QueryResult
import com.wardellbagby.sensordisabler.settings.InAppPurchaseQueryWorker
import com.wardellbagby.sensordisabler.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class QuerySkuWorkflow
@Inject constructor(
  @ApplicationContext private val androidContext: Context
) : StatefulWorkflow<Props, State, Output, Unit>() {
  data class Props(val sku: String)
  data class Output(val hasSku: Boolean)
  sealed class State {
    object LoadingBillingClient : State()
    data class LoadingInAppPurchases(val billingClient: BillingClient) : State()
  }

  override fun initialState(props: Props, snapshot: Snapshot?): State {
    return LoadingBillingClient
  }

  override fun render(renderProps: Props, renderState: State, context: RenderContext) {
    context.runningWorker(
      BillingClientSetupWorker(androidContext),
      handler = { output ->
        when (output) {
          is BillingClientOutput.BillingClientConnected -> action {
            state =
              LoadingInAppPurchases(output.client)
          }
          BillingClientOutput.BillingClientConnectionError, BillingClientOutput.BillingClientDisconnected, is BillingClientOutput.PurchasesUpdated ->
            action {
              setOutput(Output(hasSku = false))
            }
        }
      })
    when (renderState) {
      is LoadingBillingClient -> {}
      is LoadingInAppPurchases -> {
        context.runningWorker(
          InAppPurchaseQueryWorker(renderState.billingClient, Constants.SKU_TASKER),
          handler = { output ->
            when (output) {
              is ErrorQueryingPurchases -> action {
                renderState.billingClient.endConnection()
                setOutput(Output(hasSku = false))
              }
              is QueryResult -> action {
                renderState.billingClient.endConnection()
                setOutput(Output(hasSku = output.isPurchased))
              }
            }
          })
      }
    }
  }

  override fun snapshotState(state: State): Snapshot? = null
}