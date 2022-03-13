package com.wardellbagby.sensordisabler.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.querySkuDetails
import com.squareup.workflow1.Worker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PurchaseSkuWorker(
  private val billingClient: BillingClient,
  private val activity: Activity,
  private val sku: String
) : Worker<Boolean> {
  override fun run(): Flow<Boolean> {
    return flow {
      val skuDetailsResult = billingClient.querySkuDetails(
        SkuDetailsParams.newBuilder()
          .setSkusList(listOf(sku))
          .setType(BillingClient.SkuType.INAPP)
          .build()
      )

      if (skuDetailsResult.skuDetailsList.isNullOrEmpty()) {
        emit(false)
        return@flow
      }

      val purchaseHistory = billingClient.launchBillingFlow(
        activity, BillingFlowParams.newBuilder()
          .setSkuDetails(skuDetailsResult.skuDetailsList!!.first())
          .build()
      )

      emit(purchaseHistory.responseCode == BillingResponseCode.OK)
    }
  }
}