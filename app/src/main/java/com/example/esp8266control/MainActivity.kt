package com.example.esp8266control

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnOn = findViewById<Button>(R.id.btnOn)
        val btnOff = findViewById<Button>(R.id.btnOff)

        btnOn.setOnClickListener { sendMockCommand("http://localhost:8080") }
        btnOff.setOnClickListener { sendMockCommand("https://localhost:8080") }
    }

    private fun sendMockCommand(url: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        Thread {
            try {
                val response: Response = client.newCall(request).execute()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this, "Komut gönderildi: $url", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Komut başarısız: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                runOnUiThread { Toast.makeText(this, "Bağlantı hatası: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }
}
