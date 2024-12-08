package com.example.esp8266control

import Room
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment(private val rooms: List<Room>, private val isAdmin: Boolean) : Fragment() {

    private var containerLayout: LinearLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        containerLayout = view.findViewById(R.id.roomContainer)

        // Logout butonunu tanımla
        val logoutButton = view.findViewById<Button>(R.id.btnLogout)
        logoutButton.setOnClickListener {
            (activity as MainActivity).logout() // MainActivity'deki logout fonksiyonunu çağır
        }

        displayRooms()
        return view
    }

    fun updateRooms() {
        containerLayout?.removeAllViews()
        displayRooms()
    }

    private fun displayRooms() {
        containerLayout?.removeAllViews()

        for (room in rooms) {
            // Admin ise tüm odaları göster, değilse sadece görünür odaları
            if (isAdmin || room.visible) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(8, 8, 8, 8)
                }

                val button = Button(requireContext()).apply {
                    text = room.name
                    setBackgroundColor(resources.getColor(android.R.color.black)) // Başlangıç rengi

                    setOnClickListener {
                        // Butona basıldığında önce yeşil yap
                        setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                        updateLedState(true) // LED'i aç

                        // 1 saniye sonra beyaz yap
                        postDelayed({
                            setBackgroundColor(resources.getColor(android.R.color.black))
                            updateLedState(false) // LED'i kapat
                        }, 500)
                    }
                }

                row.addView(button)
                containerLayout?.addView(row)
            }
        }
    }

    private fun updateLedState(turnOn: Boolean) {
        val database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
        val ledStateRef = database.getReference("led/state")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ledStateRef.setValue(if (turnOn) 1 else 0).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                requireContext(),
                                "LED state updated to ${if (turnOn) "ON" else "OFF"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                requireContext(),
                                "Failed to update LED state",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
