package com.example.esp8266control

import Room
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingsFragment : Fragment() {

    private val rooms = mutableListOf<Room>()
    private lateinit var loggedInEmail: String

    companion object {
        fun newInstance(email: String): SettingsFragment {
            val fragment = SettingsFragment()
            val args = Bundle()
            args.putString("email", email)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loggedInEmail = arguments?.getString("email") ?: "default@example.com"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val containerLayout = view.findViewById<LinearLayout>(R.id.settingsContainer)
        val addRoomButton = view.findViewById<Button>(R.id.btnAddRoom)

        addRoomButton.setOnClickListener {
            showAddRoomDialog(containerLayout)
        }

        loadRoomsFromFirebase(containerLayout)
        return view
    }

    private fun loadRoomsFromFirebase(containerLayout: LinearLayout) {
        val database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("rooms")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rooms.clear()
                for (roomSnapshot in snapshot.children) {
                    val name = roomSnapshot.child("name").getValue(String::class.java) ?: ""
                    val ip = roomSnapshot.child("ip").getValue(String::class.java) ?: ""
                    val visible = roomSnapshot.child("visible").getValue(Boolean::class.java) ?: true
                    rooms.add(Room(name, ip, visible))
                }
                listRooms(containerLayout)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Veriler yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
                    updateRoomVisibilityInFirebase(room, isChecked)
                }
            }

            val editButton = Button(requireContext()).apply {
                text = "Edit"
                setOnClickListener {
                    showEditRoomDialog(room, containerLayout)
                }
            }

            val deleteButton = Button(requireContext()).apply {
                text = "Delete"
                setOnClickListener {
                    showDeleteConfirmationDialog(room, containerLayout)
                }
            }

            row.addView(textView)
            row.addView(toggleButton)
            row.addView(editButton)
            row.addView(deleteButton)
            containerLayout.addView(row)
        }
    }


    private fun showDeleteConfirmationDialog(room: Room, containerLayout: LinearLayout) {
        AlertDialog.Builder(requireContext())
            .setTitle("Odayı Sil")
            .setMessage("${room.name} adlı odayı silmek istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                deleteRoomFromFirebase(room, containerLayout)
            }
            .setNegativeButton("Hayır") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }


    private fun deleteRoomFromFirebase(room: Room, containerLayout: LinearLayout) {
        val database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
        val roomRef = database.getReference("rooms/${room.name}")

        roomRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                rooms.remove(room)
                listRooms(containerLayout)
                Toast.makeText(requireContext(), "${room.name} başarıyla silindi.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Silme işlemi başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun updateRoomVisibilityInFirebase(room: Room, isVisible: Boolean) {
        val database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
        val roomRef = database.getReference("rooms/${room.name}/visible")
        roomRef.setValue(isVisible)
    }

    private fun showAddRoomDialog(containerLayout: LinearLayout) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_room, null)
        val etRoomName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val etRoomOutput = dialogView.findViewById<EditText>(R.id.etRoomIp)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Yeni Oda Ekle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val roomName = etRoomName.text.toString()
                val espOutput = etRoomOutput.text.toString()

                if (roomName.isNotEmpty() && espOutput.isNotEmpty()) {
                    val newRoom = Room(roomName, espOutput, true)
                    addRoomToFirebase(newRoom)
                } else {
                    Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }

    private fun addRoomToFirebase(room: Room) {
        val database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
        val roomRef = database.getReference("rooms/${room.name}")

        // Oda bilgilerini ekle ve state başlangıç değerini 0 yap
        roomRef.setValue(room).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // State alanını ekle
                roomRef.child("state").setValue(0).addOnCompleteListener { stateTask ->
                    if (stateTask.isSuccessful) {
                        Toast.makeText(requireContext(), "${room.name} başarıyla eklendi ve state 0 olarak ayarlandı.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "State eklenirken hata: ${stateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Oda eklenirken hata: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showEditRoomDialog(room: Room, containerLayout: LinearLayout) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_room, null)
        val etRoomName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val etRoomOutput = dialogView.findViewById<EditText>(R.id.etRoomIp)

        etRoomName.setText(room.name)
        etRoomOutput.setText(room.ip)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Odayı Düzenle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val updatedName = etRoomName.text.toString()
                val updatedIp = etRoomOutput.text.toString()

                if (updatedName.isNotEmpty() && updatedIp.isNotEmpty()) {
                    updateRoomInFirebase(room, updatedName, updatedIp)

                    val index = rooms.indexOf(room)
                    if (index != -1) {
                        rooms[index] = room.copy(name = updatedName, ip = updatedIp)
                    }
                    listRooms(containerLayout)
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

    private fun updateRoomInFirebase(room: Room, newName: String, newIp: String) {
        val database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
        val roomRef = database.getReference("rooms/${room.name}")

        roomRef.removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                val newRoomRef = database.getReference("rooms/$newName")
                val updatedRoom = room.copy(name = newName, ip = newIp)
                newRoomRef.setValue(updatedRoom).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "$newName başarıyla güncellendi.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Hata: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Oda güncellenemedi: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
