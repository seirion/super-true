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

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `watchlist` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `cached_holdings` ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'KRW'"
        )
        db.execSQL(
            "ALTER TABLE `cached_account_summaries` ADD COLUMN `usdKrwRate` REAL"
        )
    }
}

// cached_holdings.quantity 를 소수점 수량(REAL)으로 변경: SQLite 는 컬럼 타입 변경이 불가해 테이블 재생성
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cached_holdings_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `accountId` INTEGER NOT NULL,
                `code` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `avgPrice` REAL NOT NULL,
                `currentPrice` REAL,
                `evaluationAmount` REAL NOT NULL,
                `profitAmount` REAL NOT NULL,
                `profitRate` REAL NOT NULL,
                `currency` TEXT NOT NULL DEFAULT 'KRW'
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `cached_holdings_new`
                (`id`, `accountId`, `code`, `name`, `quantity`, `avgPrice`, `currentPrice`,
                 `evaluationAmount`, `profitAmount`, `profitRate`, `currency`)
            SELECT `id`, `accountId`, `code`, `name`, `quantity`, `avgPrice`, `currentPrice`,
                   `evaluationAmount`, `profitAmount`, `profitRate`, `currency`
            FROM `cached_holdings`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `cached_holdings`")
        db.execSQL("ALTER TABLE `cached_holdings_new` RENAME TO `cached_holdings`")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `cached_holdings` ADD COLUMN `prevClose` REAL")
    }
}

/** 등록된 모든 Migration 목록 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
)
