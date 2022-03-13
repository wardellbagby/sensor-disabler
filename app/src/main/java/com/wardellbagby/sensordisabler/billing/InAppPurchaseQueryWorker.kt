package com.wardellbagby.sensordisabler.settings

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.queryPurchaseHistory
import com.squareup.workflow1.Worker
import com.wardellbagby.sensordisabler.settings.InAppPurchaseQueryOutput.ErrorQueryingPurchases
import com.wardellbagby.sensordisabler.settings.InAppPurchaseQueryOutput.QueryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class InAppPurchaseQueryOutput {
  object ErrorQueryingPurchases : InAppPurchaseQueryOutput()
  data class QueryResult(val isPurchased: Boolean) : InAppPurchaseQueryOutput()
}

class InAppPurchaseQueryWorker(
  private val billingClient: BillingClient,
  private val sku: String
) : Worker<InAppPurchaseQueryOutput> {
  override fun run(): Flow<InAppPurchaseQueryOutput> {
    return flow {
      val purchaseHistory = billingClient.queryPurchaseHistory(SkuType.INAPP)

      if (purchaseHistory.billingResult.responseCode != BillingResponseCode.OK)
        emit(ErrorQueryingPurchases)
      else {
        val skus = purchaseHistory
          .purchaseHistoryRecordList
          ?.map { it.skus }
          ?.flatten()

        emit(QueryResult(skus?.contains(sku) ?: false))
      }
    }
  }
}