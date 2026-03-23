package com.blueth.guard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permission_events")
data class PermissionEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val permission: String,
    val permissionGroup: String,
    val timestamp: Long,
    val isGranted: Boolean
)
