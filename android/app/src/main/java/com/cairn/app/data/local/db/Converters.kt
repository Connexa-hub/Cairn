package com.cairn.app.data.local.db

import androidx.room.TypeConverter
import com.cairn.app.data.local.entity.CallType

class Converters {
    @TypeConverter
    fun fromCallType(value: CallType): String = value.name

    @TypeConverter
    fun toCallType(value: String): CallType = CallType.valueOf(value)
}
