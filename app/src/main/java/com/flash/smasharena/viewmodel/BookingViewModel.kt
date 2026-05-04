package com.smasharena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smasharena.data.BookingRepository
import com.smasharena.data.BookingResult
import com.smasharena.data.Court
import com.smasharena.data.User
import com.smasharena.util.TimeFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BookingUiState(
    val court: Court? = null,
    val date: LocalDate = LocalDate.now(),
    val startHour: Int = 18,
    val startMinute: Int = 0,
    val durationMinutes: Int = 60,
    val isSubmitting: Boolean = false,
    val errorRes: Int? = null,
    val confirmedBookingId: Long? = null,
)

class BookingViewModel(
    private val repo: BookingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BookingUiState())
    val state: StateFlow<BookingUiState> = _state.asStateFlow()

    fun load(courtId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(court = repo.courtById(courtId))
        }
    }

    fun setDate(date: LocalDate) { _state.value = _state.value.copy(date = date) }
    fun setStart(hour: Int, minute: Int) { _state.value = _state.value.copy(startHour = hour, startMinute = minute) }
    fun setDuration(min: Int) { _state.value = _state.value.copy(durationMinutes = min) }

    fun confirm(currentUser: User) {
        val s = _state.value
        val court = s.court ?: return
        val startEpoch = TimeFormat.atHourMinute(s.date, s.startHour, s.startMinute)
        _state.value = s.copy(isSubmitting = true, errorRes = null)
        viewModelScope.launch {
            val result = repo.book(
                userId = currentUser.id,
                courtId = court.id,
                startEpochMs = startEpoch,
                durationMinutes = s.durationMinutes,
            )
            _state.value = _state.value.copy(
                isSubmitting = false,
                errorRes = result.toErrorStringRes(),
                confirmedBookingId = (result as? BookingResult.Success)?.bookingId,
            )
        }
    }

    fun clearError() { _state.value = _state.value.copy(errorRes = null) }

    class Factory(private val repo: BookingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BookingViewModel(repo) as T
    }
}

/**
 * Map a [BookingResult] to a user-facing string resource. Keeping this near
 * the ViewModel rather than inside the repository keeps the data layer free
 * of any Android dependency.
 */
private fun BookingResult.toErrorStringRes(): Int? = when (this) {
    is BookingResult.Success -> null
    BookingResult.InvalidDuration -> com.smasharena.R.string.error_invalid_duration
    BookingResult.InPast -> com.smasharena.R.string.error_in_past
    BookingResult.SlotTaken -> com.smasharena.R.string.error_slot_taken
    BookingResult.PeakReservedForPremium -> com.smasharena.R.string.error_peak_premium_only
    is BookingResult.DailyCapExceeded -> com.smasharena.R.string.error_daily_cap_exceeded
    BookingResult.UnknownUser -> com.smasharena.R.string.error_unknown
}
