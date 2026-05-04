package com.flash.smasharena.di

import android.content.Context
import androidx.room.Room
import com.flash.smasharena.data.BookingDao
import com.flash.smasharena.data.CourtDao
import com.flash.smasharena.data.SmashArenaDatabase
import com.flash.smasharena.data.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmashArenaDatabase =
        Room.databaseBuilder(context, SmashArenaDatabase::class.java, "smash_arena.db")
            .build()

    @Provides fun provideBookingDao(db: SmashArenaDatabase): BookingDao = db.bookingDao()
    @Provides fun provideCourtDao(db: SmashArenaDatabase): CourtDao = db.courtDao()
    @Provides fun provideUserDao(db: SmashArenaDatabase): UserDao = db.userDao()
}
