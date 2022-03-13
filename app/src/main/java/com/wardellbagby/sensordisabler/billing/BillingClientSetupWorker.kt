package com.wardellbagby.sensordisabler.billing

import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.squareup.workflow1.Worker
import com.wardellbagby.sensordisabler.billing.BillingClientOutput.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class BillingClientOutput {
  data class BillingClientConnected(val client: BillingClient) : BillingClientOutput()
  object BillingClientConnectionError : BillingClientOutput()
  object BillingClientDisconnected : BillingClientOutput()
  data class PurchasesUpdated(
    val result: BillingResult,
    val purchases: List<Purchase>?
  ) : BillingClientOutput()
}

class BillingClientSetupWorker(
  private val context: Context
) : Worker<BillingClientOutput> {
  override fun run(): Flow<BillingClientOutput> {
    return callbackFlow {
      val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        trySend(PurchasesUpdated(billingResult, purchases))
      }
      val billingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases()
        .setListener(purchasesUpdatedListener)
        .build()

      billingClient.startConnection(object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: BillingResult) {
          if (billingResult.responseCode == BillingResponseCode.OK) {
            trySend(BillingClientConnected(billingClient))
          } else {
            trySend(BillingClientConnectionError)
          }
        }

        override fun onBillingServiceDisconnected() {
          trySend(BillingClientDisconnected)
          billingClient.endConnection()
          close()
        }
      })
      awaitClose { billingClient.endConnection() }
    }
  }
}