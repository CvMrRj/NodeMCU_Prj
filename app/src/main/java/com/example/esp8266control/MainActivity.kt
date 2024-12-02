package com.example.esp8266control

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var espIp = "http://192.168.1.100" // Varsayılan IP adresi
    private var deviceName = "Varsayılan Cihaz" // Varsayılan isim
    private var isOn = false // LED durumu (ON/OFF)
    private val correctPassword = "123456" // Şifre

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toggleButton = findViewById<Button>(R.id.toggleButton)
        val editButton = findViewById<Button>(R.id.editButton)

        // Başlangıçta OFF durumuna ayarla
        updateButtonState(toggleButton)

        // ON/OFF Butonu
        toggleButton.setOnClickListener {
            isOn = !isOn // Durumu değiştir
            updateButtonState(toggleButton) // Buton durumunu güncelle
            sendCommandToESP() // ESP'ye komut gönder
        }

        // Edit Butonu
        editButton.setOnClickListener {
            showPasswordDialog()
        }
    }

    private fun updateButtonState(button: Button) {
        if (isOn) {
            button.text = "ON"
            button.setBackgroundColor(Color.GREEN)
        } else {
            button.text = "OFF"
            button.setBackgroundColor(Color.RED)
        }
    }

    private fun showPasswordDialog() {
        // Şifre diyaloğunu oluştur
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_password, null)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Şifre Girişi")
            .setView(dialogView)
            .setPositiveButton("Onayla") { _, _ ->
                val enteredPassword = etPassword.text.toString()
                if (enteredPassword == correctPassword) {
                    showEditDialog() // Şifre doğruysa düzenleme ekranını aç
                } else {
                    Toast.makeText(this, "Hatalı Şifre!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun showEditDialog() {
        // Düzenleme ekranı diyaloğu
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etIp = dialogView.findViewById<EditText>(R.id.etIp)

        // Mevcut isim ve IP adresini EditText'lere yaz
        etName.setText(deviceName)
        etIp.setText(espIp.removePrefix("http://"))

        val dialog = AlertDialog.Builder(this)
            .setTitle("ESP Ayarları")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val name = etName.text.toString()
                val ip = etIp.text.toString()

                if (ip.isNotEmpty()) {
                    deviceName = name
                    espIp = "http://$ip"
                    Toast.makeText(this, "$deviceName için IP adresi güncellendi: $espIp", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "IP adresi boş olamaz!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun sendCommandToESP() {
        // Burada ESP ile iletişim kurulacak. Örnek olarak bir log:
        val command = if (isOn) "/on" else "/off"
        Toast.makeText(this, "ESP'ye gönderildi: $espIp$command", Toast.LENGTH_SHORT).show()
    }
}
