package com.example.esp8266control

import Room
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment

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
                val button = Button(requireContext()).apply {
                    text = room.name
                    setBackgroundColor(
                        if (room.isOn) resources.getColor(android.R.color.holo_green_light)
                        else resources.getColor(android.R.color.holo_red_light)
                    )
                    setOnClickListener {
                        room.isOn = !room.isOn
                        setBackgroundColor(
                            if (room.isOn) resources.getColor(android.R.color.holo_green_light)
                            else resources.getColor(android.R.color.holo_red_light)
                        )
                    }
                }
                containerLayout?.addView(button)
            }
        }
    }
}
