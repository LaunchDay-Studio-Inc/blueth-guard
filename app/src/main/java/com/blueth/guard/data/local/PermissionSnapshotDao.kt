package com.blueth.guard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PermissionSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: PermissionSnapshot)

    @Query("SELECT * FROM permission_snapshots WHERE packageName = :packageName")
    suspend fun getSnapshot(packageName: String): PermissionSnapshot?

    @Query("SELECT * FROM permission_snapshots")
    suspend fun getAllSnapshots(): List<PermissionSnapshot>
}
