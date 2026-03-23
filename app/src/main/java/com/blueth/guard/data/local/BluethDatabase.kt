package com.blueth.guard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PermissionEvent::class,
        PermissionSnapshot::class,
        NetworkUsageEntry::class,
        InstallEvent::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BluethDatabase : RoomDatabase() {
    abstract fun permissionEventDao(): PermissionEventDao
    abstract fun permissionSnapshotDao(): PermissionSnapshotDao
    abstract fun networkEventDao(): NetworkEventDao
    abstract fun installEventDao(): InstallEventDao
}
