package com.wardellbagby.sensordisabler.billing

import com.squareup.workflow1.Snapshot
import com.squareup.workflow1.StatefulWorkflow
import com.squareup.workflow1.action
import com.squareup.workflow1.runningWorker
import com.wardellbagby.sensordisabler.billing.BillingClientHelper.Event.*
import com.wardellbagby.sensordisabler.modals.ModalScreen
import javax.inject.Inject

class BillingModalWorkflow
@Inject constructor(
  private val billingClientHelper: BillingClientHelper
) : StatefulWorkflow<Unit, BillingModalWorkflow.State, Nothing, ModalScreen?>() {
  data class State(val lastEvent: BillingClientHelper.Event? = null)

  override fun initialState(props: Unit, snapshot: Snapshot?): State = State()

  override fun render(renderProps: Unit, renderState: State, context: RenderContext): ModalScreen? {
    context.runningWorker(billingClientHelper.asWorker()) {
      action {
        state = State(lastEvent = it)
      }
    }

    return when (renderState.lastEvent) {
      Error -> PurchaseErrorRendering(
        onClose = context.eventHandler {
          state = State(lastEvent = null)
        }
      ).let { ModalScreen(it) }
      null, Connected, Disconnected, is PurchasesUpdated -> null
    }
  }

  override fun snapshotState(state: State): Snapshot? = null
}