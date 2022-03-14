package com.wardellbagby.sensordisabler.settings

import android.content.Context
import android.hardware.Sensor
import com.squareup.workflow1.*
import com.squareup.workflow1.WorkflowAction.Companion.noAction
import com.wardellbagby.sensordisabler.R
import com.wardellbagby.sensordisabler.billing.BillingClientHelper
import com.wardellbagby.sensordisabler.billing.BillingClientHelper.Event.Error
import com.wardellbagby.sensordisabler.billing.BillingClientHelper.Event.PurchasesUpdated
import com.wardellbagby.sensordisabler.modals.DualLayer
import com.wardellbagby.sensordisabler.settings.Output.Closed
import com.wardellbagby.sensordisabler.settings.ProStatus.*
import com.wardellbagby.sensordisabler.settings.SettingsRow.ButtonRow
import com.wardellbagby.sensordisabler.settings.SettingsRow.CheckboxRow
import com.wardellbagby.sensordisabler.settings.SettingsWorkflow.Props
import com.wardellbagby.sensordisabler.settings.State.*
import com.wardellbagby.sensordisabler.settings.filtering.settings.FilterSettingsWorkflow
import com.wardellbagby.sensordisabler.settings.filtering.type.FilterTypeWorkflow
import com.wardellbagby.sensordisabler.toolbar.NavigationIcon.BACK
import com.wardellbagby.sensordisabler.toolbar.ToolbarProps
import com.wardellbagby.sensordisabler.toolbar.ToolbarRendering
import com.wardellbagby.sensordisabler.toolbar.ToolbarWorkflow
import com.wardellbagby.sensordisabler.util.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

sealed class State {
  abstract val proStatus: ProStatus

  object LoadingInAppPurchases : State() {
    override val proStatus: ProStatus = UNKNOWN
  }

  data class EditingSettings(
    override val proStatus: ProStatus,
  ) : State()

  data class AttemptingPurchase(
    override val proStatus: ProStatus,
    val sku: String,
    val shouldConsume: Boolean
  ) : State()

  data class ChoosingFilterType(
    override val proStatus: ProStatus
  ) : State()

  data class ChangingFilterSettings(
    override val proStatus: ProStatus
  ) : State()
}

sealed class Output {
  object Closed : Output()
}

enum class ProStatus {
  PURCHASED,
  NOT_PURCHASED,
  UNKNOWN
}

data class SettingsSection(
  val header: String,
  val rows: List<SettingsRow>
)

sealed class SettingsRow {
  data class ButtonRow(
    val title: String,
    val subtitle: String = "",
    val onClick: () -> Unit = {}
  ) : SettingsRow()

  data class CheckboxRow(
    val title: String,
    val subtitle: String = "",
    val isChecked: Boolean,
    val onCheckChanged: (isChecked: Boolean) -> Unit = {}
  ) : SettingsRow()
}

class SettingsWorkflow
@Inject constructor(
  private val filterTypeWorkflow: FilterTypeWorkflow,
  private val filterSettingsWorkflow: FilterSettingsWorkflow,
  private val billingClientHelper: BillingClientHelper,
  @ApplicationContext private val androidContext: Context
) : StatefulWorkflow<Props, State, Output, DualLayer<*>>() {
  data class Props(val sensors: List<Sensor>)

  override fun initialState(
    props: Props,
    snapshot: Snapshot?
  ): State = LoadingInAppPurchases

  override fun render(
    renderProps: Props,
    renderState: State,
    context: RenderContext
  ): DualLayer<*> {
    context.runningWorker(billingClientHelper.asWorker()) {
      when (it) {
        is Error -> action {
          state = EditingSettings(state.proStatus)
        }
        is PurchasesUpdated -> action {
          if (state is EditingSettings || state is AttemptingPurchase) {
            state = LoadingInAppPurchases
          }
        }
        else -> noAction()
      }
    }
    val toolbar = context.renderChild(
      ToolbarWorkflow,
      props = ToolbarProps(
        title = androidContext.resources.getString(R.string.menu_settings),
        navigationIcon = BACK,
        onNavigationIconClicked = context.eventHandler {
          setOutput(Closed)
        }),
    )
    return when (renderState) {
      is LoadingInAppPurchases -> {
        context.runningWorker(billingClientHelper.getInAppPurchases()) {
          action {
            val isPro = it.contains(Constants.SKU_TASKER)
            ProUtil.setProStatus(androidContext, isPro)
            state = EditingSettings(
              proStatus = if (isPro) PURCHASED else NOT_PURCHASED,
            )
          }
        }
        DualLayer(
          base = SettingsScreen(toolbar = toolbar,
            sections = listOf(),
            onBack = { context.actionSink.send(action { setOutput(Closed) }) }
          ),
          modal = LoadingScreen
        )
      }
      is AttemptingPurchase -> {
        context.runningSideEffect(renderState.sku) {
          billingClientHelper.attemptPurchase(renderState.sku)
        }

        DualLayer(
          base = settingsScreen(
            toolbar = toolbar,
            renderState = renderState,
            onBack = context.eventHandler { setOutput(Closed) }
          ),
          modal = LoadingScreen
        )
      }
      is ChoosingFilterType ->
        DualLayer(
          base = settingsScreen(
            toolbar = toolbar,
            renderState = renderState
          ),
          modal = context.renderChild(
            filterTypeWorkflow,
            props = androidContext.getFilterType()
          ) {
            action {
              androidContext.setFilterType(it)
              state = EditingSettings(renderState.proStatus)
            }
          }

        )
      is ChangingFilterSettings -> DualLayer(
        base = context.renderChild(
          filterSettingsWorkflow,
          props = FilterSettingsWorkflow.Props(renderProps.sensors)
        ) {
          action { state = EditingSettings(state.proStatus) }
        }
      )
      is EditingSettings -> settingsScreen(
        toolbar = toolbar,
        renderState = renderState,
        onPurchasePro = context.eventHandler {
          state = AttemptingPurchase(state.proStatus, Constants.SKU_TASKER, shouldConsume = false)
        },
        onFreeloadingChanged = context.eventHandler { isChecked ->
          ProUtil.setFreeloadStatus(androidContext, isChecked)
        },
        onFilterTypeClicked = context.eventHandler {
          state = ChoosingFilterType(state.proStatus)
        },
        onFilterSettingsClicked = context.eventHandler {
          state = ChangingFilterSettings(state.proStatus)
        },
        onDonation = context.eventHandler { sku ->
          state = AttemptingPurchase(state.proStatus, sku, shouldConsume = true)
        },
        onBack = context.eventHandler { setOutput(Closed) }
      ).let { DualLayer(base = it) }
    }
  }

  override fun snapshotState(state: State): Nothing? = null

  private fun settingsScreen(
    toolbar: ToolbarRendering,
    renderState: State,
    onPurchasePro: () -> Unit = {},
    onFreeloadingChanged: (Boolean) -> Unit = {},
    onFilterTypeClicked: () -> Unit = {},
    onFilterSettingsClicked: () -> Unit = {},
    onDonation: (String) -> Unit = {},
    onBack: () -> Unit = {}
  ): SettingsScreen {
    return SettingsScreen(
      toolbar = toolbar,
      sections = listOfNotNull(
        SettingsSection(
          header = androidContext.getString(R.string.pro_settings_header),
          rows = listOfNotNull(
            ButtonRow(
              title = when {
                renderState.proStatus == PURCHASED -> androidContext.getString(R.string.thank_you_for_buying)
                !billingClientHelper.isAvailable -> androidContext.getString(R.string.cannot_purchase_pro)
                else -> androidContext.getString(R.string.purchase_pro_version)
              },
              subtitle = if (!billingClientHelper.isAvailable) {
                androidContext.getString(R.string.no_google_play)
              } else "",
              onClick = if (renderState.proStatus == PURCHASED || !billingClientHelper.isAvailable) {
                {} // No-op if we've already purchased pro
              } else {
                onPurchasePro
              }
            ),
            CheckboxRow(
              title = androidContext.getString(R.string.freeload),
              subtitle = androidContext.getString(R.string.freeload_description),
              isChecked = ProUtil.isFreeloaded(androidContext),
              onCheckChanged = onFreeloadingChanged
            ).takeUnless { renderState.proStatus == PURCHASED }
          )
        ),
        SettingsSection(
          header = androidContext.getString(R.string.filter_settings_header),
          rows = listOfNotNull(
            ButtonRow(
              androidContext.getString(R.string.filter_type),
              androidContext.getString(R.string.filter_type_description),
              onClick = onFilterTypeClicked
            ),
            ButtonRow(
              androidContext.getString(R.string.filter_settings),
              onClick = onFilterSettingsClicked
            ).takeIf { androidContext.getFilterType() != FilterType.None }
          )
        ).takeIf { renderState.proStatus == PURCHASED || ProUtil.isFreeloaded(androidContext) },
        SettingsSection(
          header = androidContext.getString(R.string.donations_header),
          rows = listOf(
            ButtonRow(
              androidContext.getString(R.string.donate_1),
              onClick = {
                onDonation(Constants.SKU_DONATION_1)
              }),
            ButtonRow(
              title = androidContext.getString(R.string.donate_5),
              onClick = {
                onDonation(Constants.SKU_DONATION_5)
              }),
            ButtonRow(
              title = androidContext.getString(R.string.donate_10),
              onClick = {
                onDonation(Constants.SKU_DONATION_10)
              }),
          )
        ).takeIf { billingClientHelper.isAvailable }
      ),
      onBack = onBack
    )
  }
}