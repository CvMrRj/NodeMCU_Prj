package com.example.esp8266control

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var tabs: TabLayout
    var loggedInEmail: String = "default@example.com" // Varsayılan kullanıcı

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // LoginActivity'den gelen e-posta bilgisini al
        loggedInEmail = intent.getStringExtra("EMAIL") ?: "default@example.com"

        viewPager = findViewById(R.id.viewPager)
        tabs = findViewById(R.id.tabs)

        setupTabs()
    }

    private fun setupTabs() {
        val tabAdapter = TabAdapter(supportFragmentManager)

        // HomeFragment her zaman eklenir
        tabAdapter.addFragment(HomeFragment.newInstance(loggedInEmail), "Home")

        // Sadece admin kullanıcı için SettingsFragment eklenir
        if (loggedInEmail == "ckazanoglu@gmail.com") {
            tabAdapter.addFragment(SettingsFragment.newInstance(loggedInEmail), "Settings")
        }

        viewPager.adapter = tabAdapter
        tabs.setupWithViewPager(viewPager)
    }

    // Kullanıcı çıkış yapma işlemi
    fun logout() {
        // LoginActivity'yi başlat
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // Mevcut aktiviteyi kapat
        finish()
    }

}
