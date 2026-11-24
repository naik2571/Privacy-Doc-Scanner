package com.docscan.privacy.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DocDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "privacy_docs.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_DOCS = "docs"
        const val COL_ID = "id"
        const val COL_TITLE = "title"
        const val COL_AMOUNT = "total_amount"
        const val COL_CURRENCY = "currency"
        const val COL_DATE = "date"
        const val COL_CATEGORY = "category"
        const val COL_RAW_TEXT = "raw_text"
        const val COL_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val sql = """
            CREATE TABLE $TABLE_DOCS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_AMOUNT REAL NOT NULL,
                $COL_CURRENCY TEXT NOT NULL,
                $COL_DATE TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_RAW_TEXT TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOCS")
        onCreate(db)
    }

    fun insertDoc(doc: ScannedDoc): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, doc.title)
            put(COL_AMOUNT, doc.totalAmount)
            put(COL_CURRENCY, doc.currency)
            put(COL_DATE, doc.date)
            put(COL_CATEGORY, doc.category)
            put(COL_RAW_TEXT, doc.rawText)
            put(COL_TIMESTAMP, doc.timestamp)
        }
        return db.insert(TABLE_DOCS, null, values)
    }

    fun getAllDocs(): List<ScannedDoc> {
        val list = mutableListOf<ScannedDoc>()
        val db = readableDatabase
        val cursor = db.query(TABLE_DOCS, null, null, null, null, null, "$COL_TIMESTAMP DESC")
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_ID)
            val titleIdx = it.getColumnIndexOrThrow(COL_TITLE)
            val amountIdx = it.getColumnIndexOrThrow(COL_AMOUNT)
            val currIdx = it.getColumnIndexOrThrow(COL_CURRENCY)
            val dateIdx = it.getColumnIndexOrThrow(COL_DATE)
            val catIdx = it.getColumnIndexOrThrow(COL_CATEGORY)
            val textIdx = it.getColumnIndexOrThrow(COL_RAW_TEXT)
            val timeIdx = it.getColumnIndexOrThrow(COL_TIMESTAMP)

            while (it.moveToNext()) {
                list.add(
                    ScannedDoc(
                        id = it.getLong(idIdx),
                        title = it.getString(titleIdx),
                        totalAmount = it.getDouble(amountIdx),
                        currency = it.getString(currIdx),
                        date = it.getString(dateIdx),
                        category = it.getString(catIdx),
                        rawText = it.getString(textIdx),
                        timestamp = it.getLong(timeIdx)
                    )
                )
            }
        }
        return list
    }

    fun getTotalExpenseSum(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT SUM($COL_AMOUNT) FROM $TABLE_DOCS", null)
        var sum = 0.0
        cursor.use {
            if (it.moveToFirst()) {
                sum = it.getDouble(0)
            }
        }
        return sum
    }

    fun deleteDoc(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_DOCS, "$COL_ID = ?", arrayOf(id.toString()))
    }
}
