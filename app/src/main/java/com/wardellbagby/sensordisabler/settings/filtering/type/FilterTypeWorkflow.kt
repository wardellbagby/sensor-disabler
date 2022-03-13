package com.wardellbagby.sensordisabler.settings.filtering.type

import com.squareup.workflow1.Snapshot
import com.squareup.workflow1.StatefulWorkflow
import com.wardellbagby.sensordisabler.util.FilterType
import javax.inject.Inject

class FilterTypeWorkflow
@Inject constructor() : StatefulWorkflow<FilterType, FilterType, FilterType, FilterTypeScreen>() {

  override fun render(
    renderProps: FilterType,
    renderState: FilterType,
    context: RenderContext
  ): FilterTypeScreen {
    return FilterTypeScreen(
      filterType = renderState,
      onChange = context.eventHandler { filterType ->
        state = filterType
      },
      onSave = context.eventHandler {
        setOutput(renderState)
      }
    )
  }

  override fun initialState(props: FilterType, snapshot: Snapshot?): FilterType {
    return props
  }

  override fun snapshotState(state: FilterType): Snapshot? = null
}