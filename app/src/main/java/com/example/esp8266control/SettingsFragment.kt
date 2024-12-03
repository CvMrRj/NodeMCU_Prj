package com.example.esp8266control

import Room
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class SettingsFragment(private val rooms: MutableList<Room>) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val containerLayout = view.findViewById<LinearLayout>(R.id.settingsContainer)
        val addRoomButton = view.findViewById<Button>(R.id.btnAddRoom)

        // Add Room butonunu tıklama işlemi
        addRoomButton.setOnClickListener {
            showAddRoomDialog(containerLayout)
        }

        // Odaları listele
        listRooms(containerLayout)

        return view
    }

    private fun listRooms(containerLayout: LinearLayout) {
        containerLayout.removeAllViews()
        for (room in rooms) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8, 8, 8, 8)
            }

            val textView = TextView(requireContext()).apply {
                text = room.name
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val toggleButton = Switch(requireContext()).apply {
                isChecked = room.visible
                setOnCheckedChangeListener { _, isChecked ->
                    room.visible = isChecked

                    // Home ekranını güncelle
                    val homeFragment = (activity as MainActivity).supportFragmentManager.fragments
                        .find { it is HomeFragment } as? HomeFragment
                    homeFragment?.updateRooms()
                }
            }

            val editButton = Button(requireContext()).apply {
                text = "Edit"
                setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
                setOnClickListener {
                    showEditRoomDialog(room, containerLayout)
                }
            }

            row.addView(textView)
            row.addView(editButton)
            row.addView(toggleButton)
            containerLayout.addView(row)
        }
    }

    private fun showAddRoomDialog(containerLayout: LinearLayout) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_room, null)
        val etRoomName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val etRoomIp = dialogView.findViewById<EditText>(R.id.etRoomIp)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Yeni Oda Ekle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val roomName = etRoomName.text.toString()
                val roomIp = etRoomIp.text.toString()

                if (roomName.isNotEmpty() && roomIp.isNotEmpty()) {
                    val newRoom = Room(roomName, roomIp)
                    rooms.add(newRoom)
                    listRooms(containerLayout) // Settings ekranını güncelle

                    // Home ekranını güncelle
                    val homeFragment = (activity as MainActivity).supportFragmentManager.fragments
                        .find { it is HomeFragment } as? HomeFragment
                    homeFragment?.updateRooms()
                } else {
                    Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun showEditRoomDialog(room: Room, containerLayout: LinearLayout) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_room, null)
        val etRoomName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val etRoomIp = dialogView.findViewById<EditText>(R.id.etRoomIp)

        // Mevcut bilgileri doldur
        etRoomName.setText(room.name)
        etRoomIp.setText(room.ip)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Odayı Düzenle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                room.name = etRoomName.text.toString()
                room.ip = etRoomIp.text.toString()
                listRooms(containerLayout) // Settings ekranını güncelle

                // Home ekranını güncelle
                val homeFragment = (activity as MainActivity).supportFragmentManager.fragments
                    .find { it is HomeFragment } as? HomeFragment
                homeFragment?.updateRooms()
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }
}
