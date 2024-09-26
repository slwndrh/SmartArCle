package com.example.smartarcle.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserData(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "user.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE user (token TEXT, usertype TEXT, token_expiry LONG)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS user")
        onCreate(db)
    }

    fun setToken(userType: String, token: String, tokenExpiry: Long) {
        val values = ContentValues().apply {
            put("usertype", userType)
            put("token", token)
            put("token_expiry", tokenExpiry)
        }
        val db = writableDatabase
        val result = db.update("user", values, null, null)
        if (result == 0) {
            db.insert("user", null, values)
        }
    }

    fun getToken(): String? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT token FROM user LIMIT 1", null)
        var token: String? = null
        if (cursor.moveToFirst()) {
            token = cursor.getString(cursor.getColumnIndexOrThrow("token"))
        }
        cursor.close()
        return token
    }

    fun getTokenExpiry(): Long {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT token_expiry FROM user LIMIT 1", null)
        var tokenExpiry: Long = 0
        if (cursor.moveToFirst()) {
            tokenExpiry = cursor.getLong(cursor.getColumnIndexOrThrow("token_expiry"))
        }
        cursor.close()
        return tokenExpiry
    }

    fun isTokenExpired(): Boolean {
        val tokenExpiry = getTokenExpiry()
        return System.currentTimeMillis() > tokenExpiry
    }

    fun deleteToken() {
        val db = writableDatabase
        db.delete("user", null, null)
    }

    fun setUserType(userType: String, token: String, tokenExpiry: Long) {
        val values = ContentValues().apply {
            put("usertype", userType)
            put("token", token)
            put("token_expiry", tokenExpiry)
        }
        val db = writableDatabase
        val result = db.update("user", values, null, null)
        if (result == 0) {
            db.insert("user", null, values)
        }
    }

    fun getUserType(): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM user LIMIT 1", null)
        var user: User? = null
        if (cursor.moveToFirst()) {
            val token = cursor.getString(cursor.getColumnIndexOrThrow("token"))
            val userType = cursor.getString(cursor.getColumnIndexOrThrow("usertype"))
            user = User(token, userType)
        }
        cursor.close()
        return user
    }

    data class User(val token: String, val userType: String)
}
