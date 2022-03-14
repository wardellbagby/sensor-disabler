package com.wardellbagby.sensordisabler.modals

import android.app.Dialog
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.StyleRes
import androidx.core.view.setPadding
import com.squareup.workflow1.ui.*
import com.squareup.workflow1.ui.modal.HasModals
import com.squareup.workflow1.ui.modal.ModalContainer
import com.wardellbagby.sensordisabler.views.dp

data class ModalScreen(
  val rendering: Any
)

data class DualLayer<B : Any>(
  override val beneathModals: B,
  override val modals: List<ModalScreen> = emptyList()
) : HasModals<B, ModalScreen> {
  constructor(
    base: B,
    modal: Any?
  ) : this(base, modal?.let { ModalScreen(it) })

  constructor(
    base: B,
    vararg modals: ModalScreen?
  ) : this(beneathModals = base, modals = modals.toList().filterNotNull())

  val modalRendering: Any?
    get() = modals.firstOrNull()?.rendering
}

@WorkflowUiExperimentalApi
class RenderingModalContainer @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defStyle: Int = 0,
  defStyleRes: Int = 0,
  @StyleRes private val dialogThemeResId: Int = 0
) : ModalContainer<ModalScreen>(context, attributeSet, defStyle, defStyleRes) {

  override fun buildDialog(
    initialModalRendering: ModalScreen,
    initialViewEnvironment: ViewEnvironment
  ): DialogRef<ModalScreen> {
    val dialog = object : Dialog(context, dialogThemeResId) {
      override fun onBackPressed() {
        val owner = context.onBackPressedDispatcherOwnerOrNull()
        val backPressedDispatcher = owner?.onBackPressedDispatcher
        backPressedDispatcher?.onBackPressed()
      }
    }
    val viewStub = WorkflowViewStub(context)

    dialog.setContentView(FrameLayout(context).apply {
      layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
      setPadding(16.dp)
      addView(viewStub, LayoutParams(MATCH_PARENT, MATCH_PARENT))
    })
    dialog.setCancelable(false)
    dialog.setCanceledOnTouchOutside(false)

    val ref = DialogRef(initialModalRendering, initialViewEnvironment, dialog, viewStub)
    updateDialog(ref)
    return ref
  }

  override fun updateDialog(dialogRef: DialogRef<ModalScreen>) {
    val rendering = dialogRef.modalRendering
    val stub = dialogRef.extra as WorkflowViewStub

    stub.update(rendering.rendering, dialogRef.viewEnvironment)
  }

  private class RenderingModalContainerViewFactory(
    @StyleRes private val dialogThemeResId: Int = 0
  ) : ViewFactory<DualLayer<*>> by BuilderViewFactory(
    type = DualLayer::class,
    viewConstructor = { initialRendering, initialEnv, context, _ ->
      RenderingModalContainer(context, dialogThemeResId = dialogThemeResId)
        .apply {
          layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
          bindShowRendering(initialRendering, initialEnv, ::update)
        }
    }
  )

  companion object : ViewFactory<DualLayer<*>> by RenderingModalContainerViewFactory()
}
