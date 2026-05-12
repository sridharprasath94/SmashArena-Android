package com.flash.smasharena.domain.repository

import com.flash.smasharena.domain.model.Slot
import kotlinx.coroutines.flow.Flow

interface SlotRepository {
    fun observeSlots(facilityId: String, date: String): Flow<List<Slot>>
    fun getUpcomingBookings(): Flow<List<Slot>>
    suspend fun getPastBookings(): List<Slot>
    fun getCancelledBookings(): Flow<List<Slot>>
    suspend fun bookSlot(facilityId: String, date: String, hour: Int, isFreeMembership: Boolean = false): Result<Unit>
    suspend fun cancelBooking(docId: String): Result<Unit>
    suspend fun deleteCancelledBooking(docId: String): Result<Unit>
}
