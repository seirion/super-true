package com.trueedu.tong.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * DB 버전 업 시 여기에 Migration을 추가합니다.
 * 예시: 버전 4 → 5로 올릴 때 MIGRATION_4_5를 추가하고
 *       DatabaseModule의 addMigrations()에 등록합니다.
 */

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `stocks` (
                `code` TEXT NOT NULL,
                `nameKr` TEXT NOT NULL,
                `attributes` TEXT NOT NULL,
                `kospi` INTEGER NOT NULL,
                PRIMARY KEY(`code`)
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `watchlist` (
                `code` TEXT NOT NULL,
                `nameKr` TEXT NOT NULL,
                `addedAt` INTEGER NOT NULL,
                PRIMARY KEY(`code`)
            )
            """.trimIndent()
        )
    }
}

/** 등록된 모든 Migration 목록 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_3_4,
    MIGRATION_4_5,
)
