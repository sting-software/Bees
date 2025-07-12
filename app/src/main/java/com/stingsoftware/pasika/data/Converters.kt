// app/src/main/java/com/stingsoftware/pasika/data/Converters.kt
package com.stingsoftware.pasika.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromApiaryType(value: ApiaryType) = value.name

    @TypeConverter
    fun toApiaryType(value: String) = enumValueOf<ApiaryType>(value)
}