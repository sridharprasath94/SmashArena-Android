package com.flash.smasharena.presentation.slots

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.flash.smasharena.R
import com.flash.smasharena.databinding.DialogBookingResultBinding
import dev.androidbroadcast.vbpd.viewBinding

class BookingResultDialog : DialogFragment(R.layout.dialog_booking_result) {

    private val binding by viewBinding(DialogBookingResultBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_SmashArena_ResultDialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val isBooked = args.getString(ARG_TYPE) == ResultType.BOOKED.name
        val facilityName = args.getString(ARG_FACILITY_NAME, "")
        val dateLabel = args.getString(ARG_DATE_LABEL, "")
        val timeLabel = args.getString(ARG_TIME_LABEL, "")

        val accentColor = ContextCompat.getColor(
            requireContext(),
            if (isBooked) R.color.accent_green else R.color.cancel_action,
        )
        val circleBgColor = ContextCompat.getColor(
            requireContext(),
            if (isBooked) R.color.icon_bg_success else R.color.icon_bg_cancel,
        )

        val circleBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(circleBgColor)
        }
        binding.iconCircle.background = circleBg
        binding.ivIcon.setImageResource(if (isBooked) R.drawable.ic_check else R.drawable.ic_close)
        binding.ivIcon.imageTintList = ColorStateList.valueOf(accentColor)

        binding.tvTitle.text = getString(
            if (isBooked) R.string.result_booked_title else R.string.result_cancelled_title
        )
        binding.tvFacility.text = facilityName
        binding.tvDatetime.text = "$dateLabel · $timeLabel"

        binding.btnDone.setOnClickListener { dismissAllowingStateLoss() }

        animateIconIn()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
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

    companion object {
        private const val ARG_TYPE = "type"
        private const val ARG_FACILITY_NAME = "facilityName"
        private const val ARG_DATE_LABEL = "dateLabel"
        private const val ARG_TIME_LABEL = "timeLabel"

        fun newInstance(info: BookingResultInfo) = BookingResultDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_TYPE, info.type.name)
                putString(ARG_FACILITY_NAME, info.facilityName)
                putString(ARG_DATE_LABEL, info.dateLabel)
                putString(ARG_TIME_LABEL, info.timeLabel)
            }
        }
    }
}
