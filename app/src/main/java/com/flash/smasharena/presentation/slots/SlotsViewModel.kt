package com.flash.smasharena.presentation.slots

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.model.Facility
import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.model.SlotStatus
import com.flash.smasharena.domain.model.UserProfile
import com.flash.smasharena.domain.repository.SlotRepository
import com.flash.smasharena.domain.repository.UserRepository
import com.flash.smasharena.util.BookingWindowUtils
import com.flash.smasharena.util.DateTimeUtils
import com.flash.smasharena.util.toAppError
import java.util.Calendar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlotsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val slotRepository: SlotRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    val facilityId: String = checkNotNull(savedStateHandle["facilityId"])
    private val facilityName: String = checkNotNull(savedStateHandle["facilityName"])

    private val _uiState = MutableStateFlow(SlotsUiState(facilityName = facilityName))
    val uiState: StateFlow<SlotsUiState> = _uiState.asStateFlow()

    private var userProfile: UserProfile? = null
    private var slotObserverJob: Job? = null

    init {
        viewModelScope.launch {
            userProfile = runCatching { userRepository.getOrCreateProfile() }.getOrNull()
            buildDateStrip()
            selectDate(DateTimeUtils.today())
        }
    }

    private fun buildDateStrip() {
        val tier = userProfile?.membershipTier ?: MembershipTier.NONE
        val dates = (0..13).map { offset ->
            val dateString = DateTimeUtils.dateWithOffset(offset)
            val daysUntil = DateTimeUtils.daysUntil(dateString)
            DateItem(
                dateString = dateString,
                dayLabel = DateTimeUtils.dayLabel(dateString),
                dayNumber = DateTimeUtils.dayNumber(dateString),
                isSelected = false,
                isBrowsable = BookingWindowUtils.isDateBrowsable(daysUntil, tier),
            )
        }
        _uiState.update { it.copy(dates = dates) }
    }

    fun selectDate(dateString: String) {
        _uiState.update { state ->
            state.copy(
                dates = state.dates.map { it.copy(isSelected = it.dateString == dateString) },
                selectedSlot = null,
                slots = emptyList(),
            )
        }
        observeSlots(dateString)
    }

    private fun observeSlots(date: String) {
        slotObserverJob?.cancel()
        slotObserverJob = viewModelScope.launch {
            val tier = userProfile?.membershipTier ?: MembershipTier.NONE
            val currentUid = userProfile?.uid
            val daysUntil = DateTimeUtils.daysUntil(date)

            slotRepository.observeSlots(facilityId, date).collect { slots ->
                val selectedHour = _uiState.value.selectedSlot?.hour
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val displaySlots = slots.map { slot ->
                    val isMyBooking = slot.bookedBy != null && slot.bookedBy == currentUid
                    val effective = BookingWindowUtils.effectiveStatus(
                        daysUntilSlot = daysUntil,
                        serverStatus = slot.status,
                        membershipTier = tier,
                        isMyBooking = isMyBooking,
                        slotHour = slot.hour,
                        currentHour = currentHour,
                    )
                    DisplaySlot(
                        hour = slot.hour,
                        timeLabel = slot.timeLabel,
                        effectiveStatus = effective,
                        isSelected = slot.hour == selectedHour,
                        isFreeMembership = slot.isFreeMembership,
                    )
                }
                _uiState.update { it.copy(slots = displaySlots) }
            }
        }
    }

    fun selectSlot(slot: DisplaySlot) {
        if (slot.effectiveStatus != SlotStatus.AVAILABLE && slot.effectiveStatus != SlotStatus.MY_BOOKING) return
        val alreadySelected = _uiState.value.selectedSlot?.hour == slot.hour
        val newSelected = if (alreadySelected) null else slot
        _uiState.update { state ->
            state.copy(
                slots = state.slots.map { it.copy(isSelected = it.hour == newSelected?.hour) },
                selectedSlot = newSelected,
            )
        }
    }

    fun cancelSelectedSlot() {
        val slot = _uiState.value.selectedSlot ?: return
        val dateItem = _uiState.value.dates.find { it.isSelected } ?: return
        val docId = "${facilityId}_${dateItem.dateString}_${slot.hour}"
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, error = null) }
            slotRepository.cancelBooking(docId)
                .onSuccess {
                    if (slot.isFreeMembership) {
                        runCatching { userRepository.decrementSessionsUsed(isCricket = facilityId == Facility.CRICKET_NET.id) }
                        userProfile = runCatching { userRepository.getOrCreateProfile() }.getOrNull()
                    }
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            selectedSlot = null,
                            bookingResultInfo = BookingResultInfo(
                                type = ResultType.CANCELLED,
                                facilityName = facilityName,
                                dateLabel = "${dateItem.dayLabel}, ${dateItem.dayNumber}",
                                timeLabel = slot.timeLabel,
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isCancelling = false, error = e.toAppError()) }
                }
        }
    }

    fun wouldExceedConsecutiveLimit(): Boolean {
        val state = _uiState.value
        val selectedHour = state.selectedSlot?.hour ?: return false
        if (state.selectedSlot?.effectiveStatus == SlotStatus.MY_BOOKING) return false
        val myBookingHours = state.slots
            .filter { it.effectiveStatus == SlotStatus.MY_BOOKING }
            .map { it.hour }
            .toSet()
        return exceedsConsecutiveLimit(myBookingHours, selectedHour)
    }

    private fun exceedsConsecutiveLimit(bookedHours: Set<Int>, newHour: Int): Boolean {
        val allHours = (bookedHours + newHour).sorted()
        var run = 1
        for (i in 1 until allHours.size) {
            run = if (allHours[i] == allHours[i - 1] + 1) run + 1 else 1
            if (run > 2) return true
        }
        return false
    }

    fun isFreeSession(): Boolean {
        val profile = userProfile ?: return false
        if (!profile.isMember) return false
        return if (facilityId == Facility.CRICKET_NET.id) {
            profile.cricketSessionsRemaining > 0
        } else {
            profile.sessionsRemaining > 0
        }
    }

    fun bookSelectedSlotFree() {
        val slot = _uiState.value.selectedSlot ?: return
        val dateItem = _uiState.value.dates.find { it.isSelected } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBookingFree = true, error = null) }
            slotRepository.bookSlot(facilityId, dateItem.dateString, slot.hour, isFreeMembership = true)
                .onSuccess {
                    runCatching { userRepository.incrementSessionsUsed(isCricket = facilityId == Facility.CRICKET_NET.id) }
                    userProfile = runCatching { userRepository.getOrCreateProfile() }.getOrNull()
                    _uiState.update {
                        it.copy(
                            isBookingFree = false,
                            selectedSlot = null,
                            bookingResultInfo = BookingResultInfo(
                                type = ResultType.BOOKED,
                                facilityName = facilityName,
                                dateLabel = "${dateItem.dayLabel}, ${dateItem.dayNumber}",
                                timeLabel = slot.timeLabel,
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBookingFree = false, error = e.toAppError()) }
                }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }
    fun onResultShown() = _uiState.update { it.copy(bookingResultInfo = null) }
}
