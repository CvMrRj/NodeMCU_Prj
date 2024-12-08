package com.example.esp8266control

import Room
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var tabs: TabLayout
    private var loggedInEmail: String? = null
    private val rooms = mutableListOf<Room>() // Oda listesi
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // SharedPreferencesHelper'ı başlat
        sharedPreferencesHelper = SharedPreferencesHelper(this)

        // Kaydedilen odaları yükle
        rooms.addAll(sharedPreferencesHelper.loadRooms())

        // ViewPager ve TabLayout'u tanımla
        viewPager = findViewById(R.id.viewPager)
        tabs = findViewById(R.id.tabs)

        // Kullanıcı giriş kontrolü
        loggedInEmail = sharedPreferencesHelper.getLoggedInEmail()
        if (loggedInEmail == null) {
            // Giriş yapılmamışsa login ekranını göster
            showLoginDialog()
        } else {
            // Giriş yapılmışsa sekmeleri yükle
            setupTabs()
        }
    }

    private fun showLoginDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_login, null)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Giriş Yap")
            .setView(dialogView)
            .setPositiveButton("Giriş Yap") { _, _ ->
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    if (password == "123456") { // Şifre kontrolü
                        loggedInEmail = email
                        sharedPreferencesHelper.saveLoggedInEmail(email) // Giriş yapan kullanıcıyı kaydet
                        setupTabs()
                    } else {
                        Toast.makeText(this, "Hatalı şifre. Lütfen tekrar deneyin.", Toast.LENGTH_SHORT).show()
                        showLoginDialog() // Yanlış şifre durumunda tekrar login ekranını aç
                    }
                } else {
                    Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                    showLoginDialog() // Eksik bilgi durumunda tekrar login ekranını aç
                }
            }
            .setCancelable(false) // Kullanıcı giriş yapmadan devam edemez
            .create()

        dialog.show()
    }

    private fun setupTabs() {
        val tabAdapter = TabAdapter(supportFragmentManager)
        val isAdmin = loggedInEmail == "ckazanoglu@gmail.com"

        // HomeFragment ve SettingsFragment'a admin durumunu aktar
        tabAdapter.addFragment(HomeFragment(rooms, isAdmin), "Home")
        if (isAdmin) {
            tabAdapter.addFragment(SettingsFragment(rooms), "Settings")
        }

        viewPager.adapter = tabAdapter
        tabs.setupWithViewPager(viewPager)
    }


    override fun onPause() {
        super.onPause()
        // Uygulama kapandığında odaları kaydet
        sharedPreferencesHelper.saveRooms(rooms)
    }

    // Çıkış yapma işlemi
    fun logout() {
        sharedPreferencesHelper.clearLoggedInEmail()
        finish() // Uygulamayı yeniden başlatmak için kapat
        startActivity(intent)
    }

}
