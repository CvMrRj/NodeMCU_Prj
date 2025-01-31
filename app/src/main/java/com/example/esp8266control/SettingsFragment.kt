package com.example.esp8266control

import Room
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class SettingsFragment : Fragment() {

    private val rooms = mutableListOf<Room>()
    private lateinit var database: DatabaseReference
    private var selectedPath: String = "Kart1" // Varsayılan olarak "rooms" path'i

    companion object {
        fun newInstance(email: String): SettingsFragment {
            val fragment = SettingsFragment()
            val args = Bundle()
            args.putString("email", email)
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val containerLayout = view.findViewById<LinearLayout>(R.id.settingsContainer)
        val addRoomButton = view.findViewById<Button>(R.id.btnAddRoom)
        val deviceSpinner = view.findViewById<Spinner>(R.id.device_spinner)

        // Firebase referansı
        database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference

        // Cihaz path'lerini tanımla
        val paths = listOf("Kart1", "Kart2")
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            paths
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSpinner.adapter = spinnerAdapter

        // Spinner seçim olayları
        deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPath = paths[position]
                setupFirebaseListener(containerLayout) // Seçilen path'e göre odaları yükle
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Oda ekleme butonu
        addRoomButton.setOnClickListener {
            showAddRoomDialog()
        }

        return view
    }

    private fun setupFirebaseListener(containerLayout: LinearLayout) {
        database.child(selectedPath).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rooms.clear()
                for (roomSnapshot in snapshot.children) {
                    val name = roomSnapshot.child("name").getValue(String::class.java) ?: ""
                    val ip = roomSnapshot.child("ip").getValue(String::class.java) ?: ""
                    val visible = roomSnapshot.child("visible").getValue(Boolean::class.java) ?: true
                    val timer = roomSnapshot.child("timer").getValue(Int::class.java)
                    val role = roomSnapshot.child("role").getValue(String::class.java)

                    val room = Room(name, ip, visible, timer, role, selectedPath)
                    rooms.add(room)
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
            context?.let { safeContext ->
                val row = LinearLayout(safeContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(8, 8, 8, 8)
                }

                val textView = TextView(safeContext).apply {
                    text = room.name
                    textSize = 16f
                    setTextColor(
                        ContextCompat.getColor(
                            safeContext,
                            if (room.role == "reverse") android.R.color.holo_red_dark else android.R.color.white
                        )
                    )
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val toggleButton = SwitchCompat(safeContext).apply {
                    isChecked = room.visible
                    setOnCheckedChangeListener { _, isChecked ->
                        updateRoomVisibilityInFirebase(room, isChecked)
                    }
                }

                val editButton = Button(safeContext).apply {
                    text = "EDIT"
                    setOnClickListener {
                        showEditRoomDialog(room)
                    }
                }

                val deleteButton = ImageButton(safeContext).apply {
                    layoutParams = LinearLayout.LayoutParams(100, 100)
                    setImageResource(R.drawable.ic_delete)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    setOnClickListener {
                        showDeleteConfirmationDialog(room)
                    }
                }

                // Row'a öğeleri ekle
                row.addView(textView)
                row.addView(toggleButton)
                row.addView(editButton)
                row.addView(deleteButton)

                // Row'u Container'a ekle
                containerLayout.addView(row)

                // Her oda arasında bir çizgi ekle
                val divider = View(safeContext).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2
                    ).apply {
                        setMargins(8, 4, 8, 4)
                    }
                    setBackgroundColor(ContextCompat.getColor(safeContext, android.R.color.darker_gray))
                }
                containerLayout.addView(divider)
            }
        }
    }


    private fun updateRoomVisibilityInFirebase(room: Room, isVisible: Boolean) {
        val roomRef = database.child(room.path).child(room.name).child("visible")
        roomRef.setValue(isVisible).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "${room.name} görünürlüğü güncellendi.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Görünürlük güncellenemedi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddRoomDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_room, null)
        val etRoomName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val spinnerRoomIp = dialogView.findViewById<Spinner>(R.id.spinnerRoomIp)
        val spinnerTimer = dialogView.findViewById<Spinner>(R.id.spinnerTimer)
        val checkboxRole = dialogView.findViewById<CheckBox>(R.id.checkboxRole)

        // ESP Çıkışı için Spinner seçenekleri
        val espOptions = (0..8).map { "D$it" }
        val espAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, espOptions)
        espAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRoomIp.adapter = espAdapter

        // Timer için Spinner seçenekleri
        val timerOptions = mutableListOf("Yok")
        timerOptions.addAll((1..20).map { "$it saniye" })
        val timerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timerOptions)
        timerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTimer.adapter = timerAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Yeni Oda Ekle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val roomName = etRoomName.text.toString()
                val espOutput = spinnerRoomIp.selectedItem.toString()
                val selectedTimer = spinnerTimer.selectedItemPosition
                val timerValue = if (selectedTimer == 0) null else selectedTimer
                val roleValue = if (checkboxRole.isChecked) "reverse" else null

                if (roomName.isNotEmpty() && espOutput.isNotEmpty()) {
                    val newRoom = Room(roomName, espOutput, true, timerValue, roleValue,selectedPath)
                    addRoomToFirebase(newRoom)
                } else {
                    Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }





    private fun addRoomToFirebase(room: Room) {
        val roomRef = database.child(selectedPath).child(room.name)
        roomRef.setValue(room).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val initialState = if (room.role == "reverse") 1 else 0 // Eğer role "reverse" ise state 1
                roomRef.child("state").setValue(initialState).addOnCompleteListener { stateTask ->
                    if (stateTask.isSuccessful) {
                        Toast.makeText(requireContext(), "${room.name} başarıyla eklendi.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "State ayarlanırken hata oluştu: ${stateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Oda eklenirken hata oluştu: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showEditRoomDialog(room: Room) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_room, null)
        val etRoomName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val spinnerRoomIp = dialogView.findViewById<Spinner>(R.id.spinnerRoomIp)
        val spinnerTimer = dialogView.findViewById<Spinner>(R.id.spinnerTimer)
        val checkboxRole = dialogView.findViewById<CheckBox>(R.id.checkboxRole)

        // ESP Çıkışı için Spinner seçenekleri
        val espOptions = (0..8).map { "D$it" }
        val espAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, espOptions)
        espAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRoomIp.adapter = espAdapter

        // Timer için Spinner seçenekleri
        val timerOptions = mutableListOf("Yok")
        timerOptions.addAll((1..20).map { "$it saniye" })
        val timerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timerOptions)
        timerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTimer.adapter = timerAdapter

        // Mevcut değerleri set et
        etRoomName.setText(room.name)
        val espIndex = espOptions.indexOf(room.ip)
        spinnerRoomIp.setSelection(espIndex)
        val timerIndex = room.timer?.let { timerOptions.indexOf("$it saniye") } ?: 0
        spinnerTimer.setSelection(timerIndex)
        checkboxRole.isChecked = room.role == "reverse"

        AlertDialog.Builder(requireContext())
            .setTitle("Odayı Düzenle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val updatedName = etRoomName.text.toString()
                val updatedIp = spinnerRoomIp.selectedItem.toString()
                val selectedTimer = spinnerTimer.selectedItemPosition
                val timerValue = if (selectedTimer == 0) null else selectedTimer
                val roleValue = if (checkboxRole.isChecked) "reverse" else null

                if (updatedName.isNotEmpty() && updatedIp.isNotEmpty()) {
                    updateRoomInFirebase(room, updatedName, updatedIp, timerValue, roleValue)
                } else {
                    Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }




    private fun updateRoomInFirebase(room: Room, newName: String, newIp: String, newTimer: Int?, newRole: String?) {
        val oldRoomRef = database.child(selectedPath).child(room.name)
        oldRoomRef.removeValue().addOnCompleteListener { removeTask ->
            if (removeTask.isSuccessful) {
                val newRoomRef = database.child(selectedPath).child(newName)
                val updatedRoom = room.copy(name = newName, ip = newIp, timer = newTimer, role = newRole)
                newRoomRef.setValue(updatedRoom).addOnCompleteListener { setTask ->
                    if (setTask.isSuccessful) {
                        Toast.makeText(requireContext(), "$newName başarıyla güncellendi.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Güncelleme sırasında hata oluştu: ${setTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Eski oda silinemedi: ${removeTask.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun showDeleteConfirmationDialog(room: Room) {
        AlertDialog.Builder(requireContext())
            .setTitle("Odayı Sil")
            .setMessage("${room.name} adlı odayı silmek istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                deleteRoomFromFirebase(room)
            }
            .setNegativeButton("Hayır") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun deleteRoomFromFirebase(room: Room) {
        val roomRef = database.child(room.path).child(room.name)
        roomRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                rooms.remove(room)
                Toast.makeText(requireContext(), "${room.name} başarıyla silindi.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Silinemedi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
