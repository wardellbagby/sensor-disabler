package com.wardellbagby.sensordisabler.billing

import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.ConnectionState
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.android.billingclient.api.Purchase.PurchaseState.PENDING
import com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
import com.squareup.workflow1.Worker
import com.squareup.workflow1.asWorker
import com.wardellbagby.sensordisabler.ActivityProvider
import com.wardellbagby.sensordisabler.billing.BillingClientHelper.Event.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingClientHelper
@Inject constructor(
  @ApplicationContext private val context: Context,
  private val activityProvider: ActivityProvider,
) {
  private val billingClient: BillingClient
  private val events =
    MutableSharedFlow<Event>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  init {
    events.tryEmit(Disconnected)
    billingClient = BillingClient.newBuilder(context)
      .enablePendingPurchases()
      .setListener { billingResult, purchases ->
        if (billingResult.responseCode == OK && !purchases.isNullOrEmpty()) {
          events.tryEmit(
            PurchasesUpdated(
              completed = purchases.completed(),
              pending = purchases.pending()
            )
          )
        } else {
          events.tryEmit(Error)
        }
      }
      .build()

    billingClient.startConnection(object : BillingClientStateListener {
      override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == OK) {
          events.tryEmit(Connected)
        } else {
          events.tryEmit(Error)
        }
      }

      override fun onBillingServiceDisconnected() {
        events.tryEmit(Disconnected)
        billingClient.endConnection()
      }
    })
  }


  sealed class Event {
    object Connected : Event()
    object Error : Event()
    object Disconnected : Event()
    data class PurchasesUpdated(
      val completed: List<Purchase>,
      val pending: List<Purchase>
    ) : Event()
  }

  val isAvailable: Boolean
    get() = billingClient.isValid()

  fun getInAppPurchases(): Worker<List<String>> {
    return Worker.from {
      if (!billingClient.isValid()) {
        listOf()
      } else {
        val purchaseHistory = billingClient.queryPurchaseHistory(INAPP)

        if (purchaseHistory.billingResult.responseCode != OK)
          listOf()
        else {
          purchaseHistory
            .purchaseHistoryRecordList
            ?.map { it.skus }
            ?.flatten() ?: listOf()
        }
      }
    }
  }

  suspend fun loadPendingPurchases() {
    waitUntilReady()
    val result = billingClient.queryPurchasesAsync(INAPP)
    if (result.billingResult.responseCode == OK) {
      events.tryEmit(
        PurchasesUpdated(
          completed = result.purchasesList.completed(),
          pending = result.purchasesList.pending()
        )
      )
    }
  }

  suspend fun attemptPurchase(sku: String) {
    if (!billingClient.isValid()) {
      events.tryEmit(Error)
      return
    }
    val skuDetailsResult = billingClient.querySkuDetails(
      SkuDetailsParams.newBuilder()
        .setSkusList(listOf(sku))
        .setType(INAPP)
        .build()
    )

    if (skuDetailsResult.skuDetailsList.isNullOrEmpty()) {
      events.tryEmit(Error)
      return
    }

    val activity = activityProvider.activity

    if (activity == null) {
      events.tryEmit(Error)
      return
    }

    billingClient.launchBillingFlow(
      activity, BillingFlowParams.newBuilder()
        .setSkuDetails(skuDetailsResult.skuDetailsList!!.first())
        .build()
    )
  }

  suspend fun acknowledgePurchase(purchaseToken: String, consume: Boolean) {
    if (!billingClient.isValid()) {
      return
    }

    val result = billingClient.acknowledgePurchase(
      AcknowledgePurchaseParams.newBuilder()
        .setPurchaseToken(purchaseToken)
        .build()
    )
    if (result.responseCode == OK && consume) {
      billingClient.consumePurchase(
        ConsumeParams.newBuilder()
          .setPurchaseToken(purchaseToken)
          .build()
      )
    }
  }

  fun asWorker(): Worker<Event> {
    return events.asWorker()
  }

  private suspend fun waitUntilReady() {
    // Wait until the billing client is loaded if it isn't yet.
    if (!billingClient.isValid()) {
      events.filter { it is Connected }.first()
    }
  }

  private fun BillingClient.isValid(): Boolean {
    // isReady is probably enough but I don't trust it...
    return connectionState == ConnectionState.CONNECTED && isReady
  }

  private fun List<Purchase>.completed() = filter { it.purchaseState == PURCHASED }
  private fun List<Purchase>.pending() = filter { it.purchaseState == PENDING }
}