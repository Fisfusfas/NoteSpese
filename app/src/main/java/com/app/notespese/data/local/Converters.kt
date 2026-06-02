package com.app.notespese.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString("|")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|").filter { it.isNotBlank() }
}
