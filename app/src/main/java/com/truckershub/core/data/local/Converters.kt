package com.truckershub.core.data.local

import androidx.room.TypeConverter
import com.truckershub.core.data.model.LocationType
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromLocationType(type: LocationType): String {
        return type.name
    }

    @TypeConverter
    fun toLocationType(value: String): LocationType {
        return try {
            LocationType.valueOf(value)
        } catch (e: Exception) {
            LocationType.OTHER
        }
    }
}