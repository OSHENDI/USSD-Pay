package com.example

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HistoryEntity::class], version = 2, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreateHistoryTable(db)
            }
        }

        private fun recreateHistoryTable(db: SupportSQLiteDatabase) {
            try {
                val checkCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='history'")
                val tableExists = checkCursor.count > 0
                checkCursor.close()

                if (!tableExists) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `history` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `number` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `amount` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `timestamp` TEXT NOT NULL,
                            `simName` TEXT NOT NULL,
                            `type` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    return
                }

                // Inspect columns of existing history table
                val cursor = db.query("PRAGMA table_info(`history`)")
                val existingCols = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        existingCols.add(cursor.getString(nameIndex).lowercase())
                    }
                }
                cursor.close()

                // Form copy expressions based on what actually exists in legacy table
                val idExpr = if (existingCols.contains("id")) "id" else "NULL"
                val numberExpr = if (existingCols.contains("number")) "COALESCE(number, '')" else "''"
                val nameExpr = if (existingCols.contains("name")) "COALESCE(name, '')" else "''"
                val amountExpr = if (existingCols.contains("amount")) "COALESCE(amount, '0')" else "'0'"
                val statusExpr = if (existingCols.contains("status")) "COALESCE(status, 'COMPLETED')" else "'COMPLETED'"
                val timestampExpr = if (existingCols.contains("timestamp")) "COALESCE(timestamp, '')" else "''"
                val simNameExpr = if (existingCols.contains("simname")) "COALESCE(simName, 'SIM 1')" else "'SIM 1'"
                val typeExpr = if (existingCols.contains("type")) "COALESCE(type, 'FRIEND')" else "'FRIEND'"

                // Create temp table with exact DDL Room expects (dflt_value will be null, matching Room's TableInfo)
                db.execSQL(
                    """
                    CREATE TABLE `history_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `number` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `timestamp` TEXT NOT NULL,
                        `simName` TEXT NOT NULL,
                        `type` TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // Copy all existing rows
                db.execSQL(
                    """
                    INSERT INTO `history_temp` (`id`, `number`, `name`, `amount`, `status`, `timestamp`, `simName`, `type`)
                    SELECT $idExpr, $numberExpr, $nameExpr, $amountExpr, $statusExpr, $timestampExpr, $simNameExpr, $typeExpr
                    FROM `history`
                    """.trimIndent()
                )

                // Swap tables
                db.execSQL("DROP TABLE `history`")
                db.execSQL("ALTER TABLE `history_temp` RENAME TO `history`")
            } catch (e: Exception) {
                android.util.Log.e("HistoryDatabase", "Error migrating history table", e)
            }
        }

        fun getDatabase(context: Context): HistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "ussdpay_history_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
