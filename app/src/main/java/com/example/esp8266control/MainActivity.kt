package com.example.esp8266control

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val rooms = mutableListOf<Room>() // Oda listesi
    private lateinit var roomContainer: LinearLayout // Dinamik butonları eklemek için container
    private lateinit var addButton: Button
    private val correctPassword = "123456" // Şifre
    private var loggedInEmail = "" // Giriş yapan kullanıcının e-posta adresi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addButton = findViewById(R.id.addButton)
        roomContainer = findViewById(R.id.roomContainer)

        // Kullanıcı giriş ekranını göster
        showLoginDialog()

        // "Add" butonu tıklandığında yeni bir oda eklemek için diyalog aç
        addButton.setOnClickListener {
            showAddRoomDialog()
        }
    }

    private fun showLoginDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_login, null)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Giriş Yap")
            .setView(dialogView)
            .setPositiveButton("Giriş Yap") { _, _ ->
                val email = etEmail.text.toString().trim()
                if (email.isNotEmpty()) {
                    loggedInEmail = email
                    adjustUIForUser() // Kullanıcı tipine göre UI düzenle
                } else {
                    Toast.makeText(this, "Lütfen geçerli bir e-posta girin", Toast.LENGTH_SHORT).show()
                }
            }
            .setCancelable(false) // Kullanıcı e-posta girmeden geri çıkamaz
            .create()

        dialog.show()
    }

    private fun adjustUIForUser() {
        // Yalnızca "mertcevik1994@hotmail.com" giriş yapmışsa Add Room ve Edit görünsün
        if (loggedInEmail == "mertcevik1994@hotmail.com") {
            addButton.visibility = View.VISIBLE
        } else {
            addButton.visibility = View.GONE
        }

        // Odaları yükle ve uygun şekilde göster
        loadRooms()
    }

    private fun showAddRoomDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_room, null)
        val etRoomName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val etRoomIp = dialogView.findViewById<EditText>(R.id.etRoomIp)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Yeni Oda Ekle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val roomName = etRoomName.text.toString()
                val roomIp = etRoomIp.text.toString()

                if (roomName.isNotEmpty() && roomIp.isNotEmpty()) {
                    addRoom(Room(roomName, roomIp))
                } else {
                    Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun addRoom(room: Room) {
        rooms.add(room)

        // Yeni oda için bir satır oluştur
        val roomLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 8, 8, 8)
        }

        // Oda için ON/OFF butonu
        val toggleButton = Button(this).apply {
            text = room.name
            textSize = 16f
            setBackgroundColor(
                if (room.isOn) resources.getColor(android.R.color.holo_green_light)
                else resources.getColor(android.R.color.holo_red_light)
            )
            setOnClickListener {
                toggleRoom(room, this)
            }
        }

        // "mertcevik1994@hotmail.com" ise Edit ve Delete butonlarını ekle
        if (loggedInEmail == "mertcevik1994@hotmail.com") {
            val editButton = Button(this).apply {
                text = "EDIT"
                textSize = 18f
                setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
                setOnClickListener {
                    showPasswordDialog {
                        showEditDialog(room, toggleButton)
                    }
                }
            }

            val deleteButton = Button(this).apply {
                text = "🗑"
                textSize = 24f
                setOnClickListener {
                    deleteRoom(room, roomLayout)
                }
            }

            // Edit ve Delete butonlarını Layout'a ekle
            roomLayout.addView(editButton)
            roomLayout.addView(deleteButton)
        }

        // ON/OFF butonunu Layout'a ekle
        roomLayout.addView(toggleButton)

        roomContainer.addView(roomLayout)
    }

    private fun toggleRoom(room: Room, button: Button) {
        room.isOn = !room.isOn
        button.setBackgroundColor(
            if (room.isOn) resources.getColor(android.R.color.holo_green_light)
            else resources.getColor(android.R.color.holo_red_light)
        )
    }



    private fun showPasswordDialog(onPasswordCorrect: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_password, null)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Şifre Girişi")
            .setView(dialogView)
            .setPositiveButton("Onayla") { _, _ ->
                val enteredPassword = etPassword.text.toString()
                if (enteredPassword == correctPassword) {
                    onPasswordCorrect() // Şifre doğruysa işlemi gerçekleştir
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


    private fun deleteRoom(room: Room, roomLayout: LinearLayout) {
        rooms.remove(room)
        roomContainer.removeView(roomLayout)
        Toast.makeText(this, "${room.name} silindi", Toast.LENGTH_SHORT).show()
    }

    private fun showEditDialog(room: Room, toggleButton: Button) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etIp = dialogView.findViewById<EditText>(R.id.etIp)

        etName.setText(room.name)
        etIp.setText(room.ip.removePrefix("http://"))

        val dialog = AlertDialog.Builder(this)
            .setTitle("${room.name} Ayarları")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val name = etName.text.toString()
                val ip = etIp.text.toString()

                if (ip.isNotEmpty()) {
                    room.name = name
                    room.ip = "http://$ip"
                    toggleButton.text = name
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

    private fun loadRooms() {
        // Daha önce kaydedilen odaları buraya ekleyebilirsiniz
        for (room in rooms) {
            addRoom(room)
        }
    }
}

data class Room(
    var name: String,
    var ip: String,
    var isOn: Boolean = false
)
