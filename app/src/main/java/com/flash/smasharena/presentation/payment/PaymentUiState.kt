package com.flash.smasharena.presentation.payment

import com.flash.smasharena.domain.model.AppError

enum class PaymentMethod { UPI, NET_BANKING, CARD }

sealed class PaymentEvent {
    data object PaymentSuccess : PaymentEvent()
}

data class PaymentUiState(
    val selectedMethod: PaymentMethod = PaymentMethod.UPI,
    val isProcessing: Boolean = false,
    val error: AppError? = null,
)
