package com.flash.smasharena.presentation.slots

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

        val isDestructive = type == Type.CANCEL || type == Type.LOGOUT || type == Type.INFO || type == Type.CANCEL_MEMBERSHIP

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
            }
        )
        binding.ivIcon.imageTintList = ColorStateList.valueOf(accentColor)

        binding.tvTitle.text = getString(
            when (type) {
                Type.BOOK -> R.string.dialog_confirm_book_title
                Type.CANCEL -> R.string.dialog_confirm_cancel_title
                Type.LOGOUT -> R.string.dialog_confirm_logout_title
                Type.INFO -> R.string.dialog_consecutive_limit_title
                Type.CANCEL_MEMBERSHIP -> R.string.dialog_confirm_cancel_membership_title
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
                Type.BOOK, Type.LOGOUT, Type.INFO -> R.string.dialog_cancel
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

    enum class Type { BOOK, CANCEL, LOGOUT, INFO, CANCEL_MEMBERSHIP }

    companion object {
        const val REQUEST_BOOK = "confirm_book"
        const val REQUEST_BOOK_FREE = "confirm_book_free"
        const val REQUEST_CANCEL_SLOT = "confirm_cancel_slot"
        const val REQUEST_CANCEL_BOOKING = "confirm_cancel_booking"
        const val REQUEST_LOGOUT = "confirm_logout"
        const val REQUEST_CANCEL_MEMBERSHIP = "confirm_cancel_membership"
        const val KEY_DOC_ID = "docId"

        private const val ARG_TYPE = "type"
        private const val ARG_SUBTITLE = "subtitle"
        private const val ARG_DETAIL = "detail"
        private const val ARG_REQUEST_KEY = "requestKey"
        private const val ARG_DOC_ID = "docId"

        fun book(facilityName: String, dateLabel: String, timeLabel: String) =
            build(Type.BOOK, REQUEST_BOOK, facilityName, "$dateLabel · $timeLabel")

        fun cancelSlot(facilityName: String, dateLabel: String, timeLabel: String) =
            build(Type.CANCEL, REQUEST_CANCEL_SLOT, facilityName, "$dateLabel · $timeLabel")

        fun cancelBooking(facilityName: String, dateLabel: String, timeLabel: String, docId: String) =
            build(Type.CANCEL, REQUEST_CANCEL_BOOKING, facilityName, "$dateLabel · $timeLabel", docId)

        fun logout(message: String) =
            build(Type.LOGOUT, REQUEST_LOGOUT, message, null)

        fun bookFree(facilityName: String, dateLabel: String, timeLabel: String) =
            build(Type.BOOK, REQUEST_BOOK_FREE, facilityName, "$dateLabel · $timeLabel")

        fun cancelMembership(tierDisplayName: String) =
            build(Type.CANCEL_MEMBERSHIP, REQUEST_CANCEL_MEMBERSHIP, tierDisplayName, null)

        fun limitReached(message: String) =
            build(Type.INFO, "", message, null)

        private fun build(
            type: Type,
            requestKey: String,
            subtitle: String,
            detail: String?,
            docId: String = "",
        ) = ConfirmationDialog().apply {
            arguments = bundleOf(
                ARG_TYPE to type.name,
                ARG_SUBTITLE to subtitle,
                ARG_DETAIL to detail,
                ARG_REQUEST_KEY to requestKey,
                ARG_DOC_ID to docId,
            )
        }
    }
}
