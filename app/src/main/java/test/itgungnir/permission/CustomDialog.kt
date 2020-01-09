package test.itgungnir.permission

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.dialog_custom.*

class CustomDialog private constructor() : DialogFragment() {

    private lateinit var param: SimpleDialogParam

    companion object {
        /**
         * Mark weather one instance of this dialog is being showed.
         */
        var isShowing = false

        /**
         * Create new instance of this fragment.
         */
        private fun newInstance(param: SimpleDialogParam) = CustomDialog().apply {
            this.param = param
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!)
            .setView(R.layout.dialog_custom)
            .setCancelable(false)
            .create()
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.apply {

            message.text = param.message

            confirm.setOnClickListener {
                dismissAllowingStateLoss()
                param.confirmCallback?.invoke()
            }

            cancel.setOnClickListener {
                dismissAllowingStateLoss()
                param.cancelCallback?.invoke()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        isShowing = false
        super.onDismiss(dialog)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (isShowing) {
            return
        }
        isShowing = true
        super.show(manager, tag)
    }

    data class SimpleDialogParam(
        var message: String = "",
        var confirmCallback: (() -> Unit)? = null,
        var cancelCallback: (() -> Unit)? = null
    )

    class Builder {

        private var parameter: SimpleDialogParam = SimpleDialogParam()

        fun message(text: String) = apply {
            parameter.message = text
        }

        fun onConfirm(callback: (() -> Unit)? = null) = apply {
            callback?.let { parameter.confirmCallback = it }
        }

        fun onCancel(callback: (() -> Unit)? = null) = apply {
            callback?.let { parameter.cancelCallback = it }
        }

        fun create() = newInstance(parameter)
    }
}