package com.blueth.guard.di

import android.content.Context
import androidx.room.Room
import com.blueth.guard.data.local.BatterySnapshotDao
import com.blueth.guard.data.local.BluethDatabase
import com.blueth.guard.data.local.InstallEventDao
import com.blueth.guard.data.local.NetworkEventDao
import com.blueth.guard.data.local.PermissionEventDao
import com.blueth.guard.data.local.PermissionSnapshotDao
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
    fun provideDatabase(@ApplicationContext context: Context): BluethDatabase {
        return Room.databaseBuilder(
            context,
            BluethDatabase::class.java,
            "blueth_guard_db"
        ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun providePermissionEventDao(db: BluethDatabase): PermissionEventDao =
        db.permissionEventDao()

    @Provides
    fun providePermissionSnapshotDao(db: BluethDatabase): PermissionSnapshotDao =
        db.permissionSnapshotDao()

    @Provides
    fun provideNetworkEventDao(db: BluethDatabase): NetworkEventDao =
        db.networkEventDao()

    @Provides
    fun provideInstallEventDao(db: BluethDatabase): InstallEventDao =
        db.installEventDao()

    @Provides
    fun provideBatterySnapshotDao(db: BluethDatabase): BatterySnapshotDao =
        db.batterySnapshotDao()
}
