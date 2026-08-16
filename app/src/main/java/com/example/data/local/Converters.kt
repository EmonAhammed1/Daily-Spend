package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.TransactionType
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let {
            try {
                TransactionType.valueOf(it)
            } catch (e: Exception) {
                TransactionType.EXPENSE
            }
        }
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? {
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): Int? {
        return time?.toSecondOfDay()
    }

    @TypeConverter
    fun toLocalTime(secondOfDay: Int?): LocalTime? {
        return secondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) }
    }
}
