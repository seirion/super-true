package com.trueedu.tong.db

import androidx.room.TypeConverter
import com.trueedu.tong.model.BrokerType

class BrokerTypeConverter {
    @TypeConverter
    fun fromBrokerType(value: BrokerType): String = value.name

    @TypeConverter
    fun toBrokerType(value: String): BrokerType = BrokerType.valueOf(value)
}
