package com.smasharena

import android.app.Application
import com.smasharena.data.BookingRepository
import com.smasharena.data.SmashArenaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Tiny manual DI container. We keep this in [Application] instead of pulling
 * in Hilt because the graph is small (one DB + one repository) and a hand-rolled
 * container makes the wiring obvious to readers.
 */
class SmashArenaApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val container: AppContainer by lazy { AppContainer.from(this, applicationScope) }
}

class AppContainer private constructor(
    val database: SmashArenaDatabase,
    val bookingRepository: BookingRepository,
) {
    companion object {
        fun from(app: Application, scope: CoroutineScope): AppContainer {
            val db = SmashArenaDatabase.get(app, scope)
            val repo = BookingRepository(
                bookingDao = db.bookingDao(),
                courtDao = db.courtDao(),
                userDao = db.userDao(),
            )
            return AppContainer(db, repo)
        }
    }
}
