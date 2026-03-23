package com.blueth.guard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permission_snapshots")
data class PermissionSnapshot(
    @PrimaryKey val packageName: String,
    val grantedPermissions: String,
    val snapshotTimestamp: Long
)
