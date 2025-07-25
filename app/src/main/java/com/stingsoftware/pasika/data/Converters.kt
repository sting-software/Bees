package com.stingsoftware.pasika.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromApiaryType(value: ApiaryType) = value.name

    @TypeConverter
    fun toApiaryType(value: String) = enumValueOf<ApiaryType>(value)

    @TypeConverter
    fun fromHiveRole(value: HiveRole): String {
        return value.name
    }

    @TypeConverter
    fun toHiveRole(value: String): HiveRole {
        return HiveRole.valueOf(value)
    }

    @TypeConverter
    fun fromQueenCellStatus(value: QueenCellStatus): String {
        return value.name
    }

    @TypeConverter
    fun toQueenCellStatus(value: String): QueenCellStatus {
        return QueenCellStatus.valueOf(value)
    }
}