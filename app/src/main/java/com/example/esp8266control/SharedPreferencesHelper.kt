package com.example.esp8266control

import Room
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharedPreferencesHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    private val gson = Gson()

    // Odaları kaydet
    fun saveRooms(rooms: List<Room>) {
        val json = gson.toJson(rooms)
        prefs.edit().putString("rooms", json).apply()
    }

    // Odaları yükle
    fun loadRooms(): MutableList<Room> {
        val json = prefs.getString("rooms", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Room>>() {}.type
        return gson.fromJson(json, type)

    }

    fun saveLoggedInEmail(email: String) {
        prefs.edit().putString("loggedInEmail", email).apply()
    }

    fun getLoggedInEmail(): String? {
        return prefs.getString("loggedInEmail", null)
    }

    fun clearLoggedInEmail() {
        prefs.edit().remove("loggedInEmail").apply()
    }

}
