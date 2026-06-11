package com.trueedu.tong.extensions

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private class IntPreference(
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: Int
) : ReadWriteProperty<Any, Int> {

    override fun getValue(thisRef: Any, property: KProperty<*>): Int =
        sharedPreferences.getInt(property.name, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) =
        sharedPreferences.edit()
            .putInt(property.name, value)
            .apply()
}

private class LongPreference(
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: Long
) : ReadWriteProperty<Any, Long> {

    override fun getValue(thisRef: Any, property: KProperty<*>): Long =
        sharedPreferences.getLong(property.name, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) =
        sharedPreferences.edit()
            .putLong(property.name, value)
            .apply()
}

private class FloatPreference(
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: Float
) : ReadWriteProperty<Any, Float> {

    override fun getValue(thisRef: Any, property: KProperty<*>): Float =
        sharedPreferences.getFloat(property.name, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Float) =
        sharedPreferences.edit()
            .putFloat(property.name, value)
            .apply()
}

private class BooleanPreference(
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: Boolean
) : ReadWriteProperty<Any, Boolean> {

    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean =
        sharedPreferences.getBoolean(property.name, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) =
        sharedPreferences.edit()
            .putBoolean(property.name, value)
            .apply()
}

private class StringPreference(
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: String
) : ReadWriteProperty<Any, String> {

    override fun getValue(thisRef: Any, property: KProperty<*>): String =
        sharedPreferences.getString(property.name, defaultValue)!!

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String) =
        sharedPreferences.edit()
            .putString(property.name, value)
            .apply()
}

fun SharedPreferences.string(defaultValue: String = ""): ReadWriteProperty<Any, String> =
    StringPreference(this, defaultValue)

fun SharedPreferences.int(defaultValue: Int = 0): ReadWriteProperty<Any, Int> =
    IntPreference(this, defaultValue)

fun SharedPreferences.long(defaultValue: Long = 0L): ReadWriteProperty<Any, Long> =
    LongPreference(this, defaultValue)

fun SharedPreferences.float(defaultValue: Float = 0f): ReadWriteProperty<Any, Float> =
    FloatPreference(this, defaultValue)

fun SharedPreferences.boolean(defaultValue: Boolean = false): ReadWriteProperty<Any, Boolean> =
    BooleanPreference(this, defaultValue)
