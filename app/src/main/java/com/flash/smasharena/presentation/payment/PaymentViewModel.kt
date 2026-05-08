package com.flash.smasharena.presentation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.repository.SlotRepository
import com.flash.smasharena.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val slotRepository: SlotRepository,
) : ViewModel() {

    private val facilityId: String = checkNotNull(savedStateHandle["facilityId"])
    private val date: String = checkNotNull(savedStateHandle["date"])
    private val hour: Int = checkNotNull(savedStateHandle["hour"])

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _events = Channel<PaymentEvent>(Channel.BUFFERED)
    val events: Flow<PaymentEvent> = _events.receiveAsFlow()

    fun selectMethod(method: PaymentMethod) =
        _uiState.update { it.copy(selectedMethod = method) }

    fun pay() {
        if (_uiState.value.isProcessing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            delay(1500L) // TODO: replace with real payment gateway call
            slotRepository.bookSlot(facilityId, date, hour)
                .onSuccess {
                    _uiState.update { it.copy(isProcessing = false) }
                    _events.trySend(PaymentEvent.PaymentSuccess)
                }
                .onFailure { e -> _uiState.update { it.copy(isProcessing = false, error = e.toAppError()) } }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }
}
