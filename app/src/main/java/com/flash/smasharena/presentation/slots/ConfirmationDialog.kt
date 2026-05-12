package com.flash.smasharena.presentation.slots

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.flash.smasharena.R
import com.flash.smasharena.databinding.DialogConfirmationBinding
import dev.androidbroadcast.vbpd.viewBinding

class ConfirmationDialog : DialogFragment(R.layout.dialog_confirmation) {

    private val binding by viewBinding(DialogConfirmationBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_SmashArena_ResultDialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val type = Type.valueOf(args.getString(ARG_TYPE)!!)
        val subtitle = args.getString(ARG_SUBTITLE, "")
        val detail = args.getString(ARG_DETAIL)
        val requestKey = args.getString(ARG_REQUEST_KEY, "")
        val docId = args.getString(ARG_DOC_ID, "")

        val isDestructive = type == Type.CANCEL || type == Type.LOGOUT || type == Type.INFO || type == Type.CANCEL_MEMBERSHIP || type == Type.DELETE
        val customTitle = args.getString(ARG_TITLE)

        val accentColor = ContextCompat.getColor(
            requireContext(),
            if (isDestructive) R.color.cancel_action else R.color.accent_green,
        )

        binding.iconCircle.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isDestructive) R.color.icon_bg_cancel else R.color.icon_bg_success,
                )
            )
        }
        binding.ivIcon.setImageResource(
            when (type) {
                Type.BOOK -> R.drawable.ic_check
                Type.CANCEL -> R.drawable.ic_close
                Type.LOGOUT -> R.drawable.ic_logout
                Type.INFO -> R.drawable.ic_close
                Type.CANCEL_MEMBERSHIP -> R.drawable.ic_close
                Type.DELETE -> R.drawable.ic_close
                Type.SELECT_MEMBERSHIP, Type.SCHEDULE_MEMBERSHIP -> R.drawable.ic_check
            }
        )
        binding.ivIcon.imageTintList = ColorStateList.valueOf(accentColor)

        binding.tvTitle.text = customTitle ?: getString(
            when (type) {
                Type.BOOK -> R.string.dialog_confirm_book_title
                Type.CANCEL -> R.string.dialog_confirm_cancel_title
                Type.LOGOUT -> R.string.dialog_confirm_logout_title
                Type.INFO -> R.string.dialog_consecutive_limit_title
                Type.CANCEL_MEMBERSHIP -> R.string.dialog_confirm_cancel_membership_title
                Type.DELETE -> R.string.dialog_delete_confirm
                Type.SELECT_MEMBERSHIP -> R.string.dialog_confirm_membership_title
                Type.SCHEDULE_MEMBERSHIP -> R.string.dialog_confirm_membership_title
            }
        )
        binding.tvSubtitle.text = subtitle
        binding.tvDetail.text = detail
        binding.tvDetail.isVisible = !detail.isNullOrEmpty()

        binding.btnConfirm.text = getString(
            when (type) {
                Type.BOOK -> R.string.dialog_book_confirm
                Type.CANCEL -> R.string.dialog_cancel_booking_confirm
                Type.LOGOUT -> R.string.dialog_logout_confirm
                Type.INFO -> R.string.dialog_got_it
                Type.CANCEL_MEMBERSHIP -> R.string.dialog_cancel_membership_confirm
                Type.DELETE -> R.string.dialog_delete_confirm
                Type.SELECT_MEMBERSHIP -> R.string.dialog_select_membership_confirm
                Type.SCHEDULE_MEMBERSHIP -> R.string.dialog_schedule_membership_confirm
            }
        )
        binding.btnConfirm.backgroundTintList = ColorStateList.valueOf(accentColor)
        binding.btnConfirm.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isDestructive) R.color.text_primary else R.color.background,
            )
        )
        binding.btnConfirm.setOnClickListener {
            if (type != Type.INFO) setFragmentResult(requestKey, bundleOf(KEY_DOC_ID to docId))
            dismissAllowingStateLoss()
        }

        binding.btnDismiss.isVisible = type != Type.INFO
        binding.btnDismiss.text = getString(
            when (type) {
                Type.BOOK, Type.LOGOUT, Type.INFO, Type.DELETE,
                Type.SELECT_MEMBERSHIP, Type.SCHEDULE_MEMBERSHIP -> R.string.dialog_cancel
                Type.CANCEL, Type.CANCEL_MEMBERSHIP -> R.string.dialog_keep
            }
        )
        binding.btnDismiss.setOnClickListener { dismissAllowingStateLoss() }

        animateIconIn()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun animateIconIn() {
        binding.iconCircle.scaleX = 0f
        binding.iconCircle.scaleY = 0f
        binding.iconCircle.postDelayed({
            binding.iconCircle.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(480)
                .setInterpolator(OvershootInterpolator(2.8f))
                .start()
        }, 80)
    }

    enum class Type { BOOK, CANCEL, LOGOUT, INFO, CANCEL_MEMBERSHIP, DELETE, SELECT_MEMBERSHIP, SCHEDULE_MEMBERSHIP }

    companion object {
        const val REQUEST_BOOK_FREE = "confirm_book_free"
        const val REQUEST_CANCEL_SLOT = "confirm_cancel_slot"
        const val REQUEST_CANCEL_BOOKING = "confirm_cancel_booking"
        const val REQUEST_LOGOUT = "confirm_logout"
        const val REQUEST_CANCEL_MEMBERSHIP = "confirm_cancel_membership"
        const val REQUEST_SELECT_MEMBERSHIP = "confirm_select_membership"
        const val REQUEST_SCHEDULE_MEMBERSHIP = "confirm_schedule_membership"
        const val REQUEST_DELETE_CANCELLED = "delete_cancelled"
        const val REQUEST_DELETE_CANCELLED_MULTI = "delete_cancelled_multi"
        const val KEY_DOC_ID = "docId"

        private const val ARG_TYPE = "type"
        private const val ARG_SUBTITLE = "subtitle"
        private const val ARG_DETAIL = "detail"
        private const val ARG_REQUEST_KEY = "requestKey"
        private const val ARG_DOC_ID = "docId"
        private const val ARG_TITLE = "title"

        fun cancelSlot(facilityName: String, dateLabel: String, timeLabel: String) =
            build(Type.CANCEL, REQUEST_CANCEL_SLOT, facilityName, "$dateLabel · $timeLabel")

        fun cancelBooking(facilityName: String, dateLabel: String, timeLabel: String, docId: String) =
            build(Type.CANCEL, REQUEST_CANCEL_BOOKING, facilityName, "$dateLabel · $timeLabel", docId)

        fun logout(message: String) =
            build(Type.LOGOUT, REQUEST_LOGOUT, message, null)

        fun bookFree(facilityName: String, dateLabel: String, timeLabel: String) =
            build(Type.BOOK, REQUEST_BOOK_FREE, facilityName, "$dateLabel · $timeLabel")

        fun cancelMembership(tierDisplayName: String, detail: String) =
            build(Type.CANCEL_MEMBERSHIP, REQUEST_CANCEL_MEMBERSHIP, tierDisplayName, detail)

        fun limitReached(message: String) =
            build(Type.INFO, "", message, null)

        fun selectMembership(message: String, detail: String? = null) =
            build(Type.SELECT_MEMBERSHIP, REQUEST_SELECT_MEMBERSHIP, message, detail)

        fun deleteCancelled(context: Context, docId: String) = build(
            type = Type.DELETE,
            requestKey = REQUEST_DELETE_CANCELLED,
            subtitle = context.getString(R.string.dialog_delete_cancelled_subtitle),
            detail = null,
            docId = docId,
            title = context.getString(R.string.dialog_delete_cancelled_title),
        )

        fun deleteMultipleCancelled(context: Context, count: Int) = build(
            type = Type.DELETE,
            requestKey = REQUEST_DELETE_CANCELLED_MULTI,
            subtitle = context.getString(R.string.dialog_delete_multiple_cancelled_subtitle),
            detail = null,
            title = context.getString(R.string.dialog_delete_multiple_cancelled_title, count),
        )

        fun scheduleMembership(context: Context, tierDisplayName: String) = build(
            type = Type.SCHEDULE_MEMBERSHIP,
            requestKey = REQUEST_SCHEDULE_MEMBERSHIP,
            subtitle = context.getString(R.string.dialog_schedule_membership_subtitle, tierDisplayName),
            detail = null,
            title = context.getString(R.string.dialog_schedule_membership_title, tierDisplayName),
        )

        private fun build(
            type: Type,
            requestKey: String,
            subtitle: String,
            detail: String?,
            docId: String = "",
            title: String? = null,
        ) = ConfirmationDialog().apply {
            arguments = bundleOf(
                ARG_TYPE to type.name,
                ARG_SUBTITLE to subtitle,
                ARG_DETAIL to detail,
                ARG_REQUEST_KEY to requestKey,
                ARG_DOC_ID to docId,
                ARG_TITLE to title,
            )
        }
    }
}
