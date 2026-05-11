package com.flash.smasharena.presentation.mybookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.model.Facility
import com.flash.smasharena.domain.repository.SlotRepository
import com.flash.smasharena.domain.repository.UserRepository
import com.flash.smasharena.util.DateTimeUtils
import com.flash.smasharena.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyBookingsViewModel @Inject constructor(
    private val slotRepository: SlotRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyBookingsUiState())
    val uiState: StateFlow<MyBookingsUiState> = _uiState.asStateFlow()

    private var allPastBookings: List<BookingItem> = emptyList()
    private var loadedPastMonths = 12
    private val expandedYears = mutableSetOf<Int>()
    private val expandedMonths = mutableSetOf<Pair<Int, Int>>()

    init {
        observeUpcoming()
        observeCancelled()
        loadPast()
    }

    private fun observeUpcoming() {
        viewModelScope.launch {
            slotRepository.getUpcomingBookings().collect { slots ->
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val items = slots.map { slot ->
                    val daysUntil = DateTimeUtils.daysUntil(slot.date)
                    val isExpired = daysUntil < 0 || (daysUntil == 0 && slot.hour <= currentHour)
                    BookingItem(
                        docId = slot.docId,
                        facilityId = slot.facilityId,
                        facilityDisplayName = facilityDisplayName(slot.facilityId),
                        date = slot.date,
                        displayDate = DateTimeUtils.displayDate(slot.date),
                        hour = slot.hour,
                        timeLabel = slot.timeLabel,
                        isFreeMembership = slot.isFreeMembership,
                        isExpired = isExpired,
                    )
                }
                _uiState.update { it.copy(upcomingItems = items, isUpcomingLoading = false) }
            }
        }
    }

    private fun observeCancelled() {
        viewModelScope.launch {
            slotRepository.getCancelledBookings().collect { slots ->
                val items = slots.map { slot ->
                    CancelledBookingItem(
                        docId = slot.docId,
                        facilityDisplayName = facilityDisplayName(slot.facilityId),
                        displayDate = DateTimeUtils.displayDate(slot.date),
                        timeLabel = slot.timeLabel,
                        cancelledDisplayDate = slot.cancelledAtMs
                            ?.let { DateTimeUtils.displayEpochDate(it) } ?: "",
                    )
                }
                _uiState.update { state ->
                    val updatedSelected = state.selectedCancelledIds
                        .filter { id -> items.any { it.docId == id } }.toSet()
                    state.copy(
                        cancelledItems = items.map { item ->
                            item.copy(isSelected = item.docId in updatedSelected)
                        },
                        selectedCancelledIds = updatedSelected,
                        isCancelledLoading = false,
                    )
                }
            }
        }
    }

    fun loadPast(loadMore: Boolean = false) {
        if (loadMore) loadedPastMonths += 12
        viewModelScope.launch {
            _uiState.update { it.copy(isPastLoading = true) }
            runCatching { slotRepository.getPastBookings() }
                .onSuccess { slots ->
                    val items = slots.map { slot ->
                        BookingItem(
                            docId = slot.docId,
                            facilityId = slot.facilityId,
                            facilityDisplayName = facilityDisplayName(slot.facilityId),
                            date = slot.date,
                            displayDate = DateTimeUtils.displayDate(slot.date),
                            hour = slot.hour,
                            timeLabel = slot.timeLabel,
                            isFreeMembership = slot.isFreeMembership,
                            isExpired = true,
                        )
                    }
                    allPastBookings = items
                    if (expandedYears.isEmpty() && items.isNotEmpty()) {
                        val latestYear = items.maxOf { it.date.substring(0, 4).toInt() }
                        expandedYears.add(latestYear)
                        val latestMonth = items
                            .filter { it.date.substring(0, 4).toInt() == latestYear }
                            .maxOf { it.date.substring(5, 7).toInt() }
                        expandedMonths.add(latestYear to latestMonth)
                    }
                    rebuildPastTimeline()
                    _uiState.update { it.copy(isPastLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isPastLoading = false, error = e.toAppError()) }
                }
        }
    }

    private fun rebuildPastTimeline() {
        val fromDate = DateTimeUtils.monthsAgo(loadedPastMonths)
        val visibleItems = allPastBookings.filter { it.date >= fromDate }
        val hasMore = allPastBookings.any { it.date < fromDate }
        _uiState.update { it.copy(pastTimelineItems = buildTimeline(visibleItems, hasMore)) }
    }

    private fun buildTimeline(items: List<BookingItem>, hasMore: Boolean): List<PastTimelineItem> {
        val byYear = items.groupBy { it.date.substring(0, 4).toInt() }
            .toSortedMap(reverseOrder())
        return buildList {
            for ((year, yearItems) in byYear) {
                val isYearExpanded = year in expandedYears
                add(PastTimelineItem.YearHeader(year, isYearExpanded))
                if (isYearExpanded) {
                    val byMonth = yearItems.groupBy { it.date.substring(5, 7).toInt() }
                        .toSortedMap(reverseOrder())
                    for ((month, monthItems) in byMonth) {
                        val isMonthExpanded = (year to month) in expandedMonths
                        add(PastTimelineItem.MonthHeader(year, month, DateTimeUtils.monthName(month), isMonthExpanded))
                        if (isMonthExpanded) {
                            monthItems.forEach { add(PastTimelineItem.BookingEntry(it)) }
                        }
                    }
                }
            }
            if (hasMore) add(PastTimelineItem.ShowMore)
        }
    }

    fun toggleYearExpanded(year: Int) {
        if (year in expandedYears) expandedYears.remove(year) else expandedYears.add(year)
        rebuildPastTimeline()
    }

    fun toggleMonthExpanded(year: Int, month: Int) {
        val key = year to month
        if (key in expandedMonths) expandedMonths.remove(key) else expandedMonths.add(key)
        rebuildPastTimeline()
    }

    fun cancelBooking(docId: String) {
        val item = _uiState.value.upcomingItems.find { it.docId == docId }
        viewModelScope.launch {
            _uiState.update { it.copy(cancellingDocId = docId) }
            slotRepository.cancelBooking(docId)
                .onSuccess {
                    if (item?.isFreeMembership == true) {
                        runCatching {
                            userRepository.decrementSessionsUsed(
                                isCricket = item.facilityId == Facility.CRICKET_NET.id
                            )
                        }
                    }
                    _uiState.update { it.copy(cancellingDocId = null, cancelSuccess = true) }
                }
                .onFailure { e -> _uiState.update { it.copy(cancellingDocId = null, error = e.toAppError()) } }
        }
    }

    fun toggleSelectCancelled(docId: String) {
        _uiState.update { state ->
            val newSelected = if (docId in state.selectedCancelledIds)
                state.selectedCancelledIds - docId
            else
                state.selectedCancelledIds + docId
            state.copy(
                selectedCancelledIds = newSelected,
                cancelledItems = state.cancelledItems.map { it.copy(isSelected = it.docId in newSelected) },
            )
        }
    }

    fun selectAllCancelled() {
        _uiState.update { state ->
            val allIds = state.cancelledItems.map { it.docId }.toSet()
            state.copy(
                selectedCancelledIds = allIds,
                cancelledItems = state.cancelledItems.map { it.copy(isSelected = true) },
            )
        }
    }

    fun clearCancelledSelection() {
        _uiState.update { state ->
            state.copy(
                selectedCancelledIds = emptySet(),
                cancelledItems = state.cancelledItems.map { it.copy(isSelected = false) },
            )
        }
    }

    fun deleteSingleCancelled(docId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingCancelled = true) }
            slotRepository.deleteCancelledBooking(docId)
                .onFailure { e -> _uiState.update { it.copy(error = e.toAppError()) } }
            _uiState.update { it.copy(isDeletingCancelled = false) }
        }
    }

    fun deleteSelectedCancelled() {
        val toDelete = _uiState.value.selectedCancelledIds.toList()
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingCancelled = true) }
            toDelete.forEach { docId ->
                slotRepository.deleteCancelledBooking(docId)
                    .onFailure { e -> _uiState.update { it.copy(error = e.toAppError()) } }
            }
            _uiState.update { it.copy(isDeletingCancelled = false, selectedCancelledIds = emptySet()) }
        }
    }

    fun onCancelSuccessHandled() = _uiState.update { it.copy(cancelSuccess = false) }
    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    private fun facilityDisplayName(facilityId: String): String = when (facilityId) {
        "badminton_court_1" -> "Badminton Court"
        "cricket_net_1" -> "Cricket Net"
        else -> facilityId
    }
}
