package com.blueth.guard.data.local

import androidx.room.TypeConverter
import com.blueth.guard.privacy.InstallAction

class Converters {

    @TypeConverter
    fun fromInstallAction(action: InstallAction): String = action.name

    @TypeConverter
    fun toInstallAction(value: String): InstallAction = InstallAction.valueOf(value)

    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(separator = "|||")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split("|||")
}
