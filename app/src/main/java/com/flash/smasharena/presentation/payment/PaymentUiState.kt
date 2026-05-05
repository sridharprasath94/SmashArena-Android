package com.flash.smasharena.presentation.payment

import com.flash.smasharena.domain.model.AppError

enum class PaymentMethod { UPI, NET_BANKING, CARD }

data class PaymentUiState(
    val selectedMethod: PaymentMethod = PaymentMethod.UPI,
    val isProcessing: Boolean = false,
    val paymentSuccess: Boolean = false,
    val error: AppError? = null,
)
