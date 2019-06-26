package my.itgungnir.permission

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_g_permission.*

class GPermissionDialog private constructor() : DialogFragment() {

    private lateinit var param: SimpleDialogParam

    companion object {
        private fun newInstance(param: SimpleDialogParam) = GPermissionDialog().apply {
            this.param = param
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        dialog.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        super.onActivityCreated(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_g_permission, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = 10F
        }

        message.text = param.message

        confirm.setOnClickListener {
            param.confirmCallback?.invoke()
            dismissAllowingStateLoss()
        }

        cancel.setOnClickListener {
            param.cancelCallback?.invoke()
            dismissAllowingStateLoss()
        }
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