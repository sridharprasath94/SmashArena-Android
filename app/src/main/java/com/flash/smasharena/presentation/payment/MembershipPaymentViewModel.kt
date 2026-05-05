package com.flash.smasharena.presentation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.repository.UserRepository
import com.flash.smasharena.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembershipPaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val tierName: String = checkNotNull(savedStateHandle["tierName"])
    private val isUpgrade: Boolean = checkNotNull(savedStateHandle["isUpgrade"])
    private val tier: MembershipTier = MembershipTier.valueOf(tierName)

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun selectMethod(method: PaymentMethod) =
        _uiState.update { it.copy(selectedMethod = method) }

    fun pay() {
        if (_uiState.value.isProcessing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            delay(1500L)
            runCatching {
                if (isUpgrade) userRepository.upgradeMembership(tier)
                else userRepository.purchaseMembership(tier)
            }
                .onSuccess { _uiState.update { it.copy(isProcessing = false, paymentSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(isProcessing = false, error = e.toAppError()) } }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }
}
