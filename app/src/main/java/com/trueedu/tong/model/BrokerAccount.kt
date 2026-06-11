package com.trueedu.tong.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "broker_accounts")
data class BrokerAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val brokerType: BrokerType,
    val accountNum: String,
    val appKey: String,
    val appSecret: String,
    val isSelected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
