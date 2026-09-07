package com.example.contextos.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(SEPARATOR) ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(SEPARATOR).filter { it.isNotEmpty() }
    }

    companion object {
        private const val SEPARATOR = "||_ctx_||"
    }
}
