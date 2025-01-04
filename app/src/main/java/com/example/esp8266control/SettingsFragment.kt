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
    private lateinit var loggedInEmail: String
    private lateinit var database: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener
    private var selectedPath: String = "rooms" // Varsayılan olarak "rooms" path'i

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
        val deviceSpinner = view.findViewById<Spinner>(R.id.device_spinner)

        // Firebase referansı
        database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference()

        // Cihaz path'lerini tanımla
        val paths = listOf("rooms", "rooms2")
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
            showAddRoomDialog(containerLayout)
        }

        return view
    }

    private fun setupFirebaseListener(containerLayout: LinearLayout) {
        database.child(selectedPath).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isAdded && context != null) {
                    rooms.clear()
                    for (roomSnapshot in snapshot.children) {
                        val name = roomSnapshot.child("name").getValue(String::class.java) ?: ""
                        val ip = roomSnapshot.child("ip").getValue(String::class.java) ?: ""
                        val visible = roomSnapshot.child("visible").getValue(Boolean::class.java) ?: true
                        rooms.add(Room(name, ip, visible))
                    }
                    listRooms(containerLayout)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Veriler yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
                }
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
                        showEditRoomDialog(room, containerLayout)
                    }
                }

                val deleteButton = ImageButton(safeContext).apply {
                    layoutParams = LinearLayout.LayoutParams(100, 100)
                    setImageResource(R.drawable.ic_delete)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    setOnClickListener {
                        showDeleteConfirmationDialog(room, containerLayout)
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
        // Doğru path'i seçmek için selectedPath'i kullan
        val roomRef = database.child(selectedPath).child(room.name).child("visible")
        roomRef.setValue(isVisible).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "${room.name} görünürlüğü güncellendi.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Görünürlük güncellenemedi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showAddRoomDialog(containerLayout: LinearLayout) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_room, null)
        val etRoomName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val etRoomOutput = dialogView.findViewById<EditText>(R.id.etRoomIp)
        val spinnerTimer = dialogView.findViewById<Spinner>(R.id.spinnerTimer)

        // Timer için Spinner seçeneklerini oluştur
        val timerOptions = mutableListOf("Yok")
        timerOptions.addAll((1..20).map { "$it saniye" })
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timerOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTimer.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setTitle("Yeni Oda Ekle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val roomName = etRoomName.text.toString()
                val espOutput = etRoomOutput.text.toString()
                val selectedTimer = spinnerTimer.selectedItemPosition

                if (roomName.isNotEmpty() && espOutput.isNotEmpty()) {
                    val timerValue = if (selectedTimer == 0) null else selectedTimer
                    val newRoom = Room(roomName, espOutput, true, timerValue)
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
        val roomRef = database.child(selectedPath).child(room.name) // selectedPath burada kullanılıyor
        roomRef.setValue(room).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                roomRef.child("state").setValue(0).addOnCompleteListener { stateTask ->
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


    private fun showEditRoomDialog(room: Room, containerLayout: LinearLayout) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_room, null)
        val etRoomName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val etRoomOutput = dialogView.findViewById<EditText>(R.id.etRoomIp)
        val spinnerTimer = dialogView.findViewById<Spinner>(R.id.spinnerTimer)

        // Timer için Spinner seçeneklerini oluştur
        val timerOptions = mutableListOf("Yok")
        timerOptions.addAll((1..20).map { "$it saniye" })
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timerOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTimer.adapter = adapter

        // Mevcut oda bilgilerini doldur
        etRoomName.setText(room.name)
        etRoomOutput.setText(room.ip)

        // Timer bilgisi al ve Spinner'ı güncelle
        val timerRef = database.child(room.name).child("timer")
        timerRef.get().addOnSuccessListener { snapshot ->
            val timerValue = snapshot.getValue(Int::class.java)
            val selectedPosition = timerValue?.let { timerOptions.indexOf("$it saniye") } ?: 0
            spinnerTimer.setSelection(selectedPosition)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Odayı Düzenle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val updatedName = etRoomName.text.toString()
                val updatedIp = etRoomOutput.text.toString()
                val selectedTimer = spinnerTimer.selectedItemPosition

                if (updatedName.isNotEmpty() && updatedIp.isNotEmpty()) {
                    val timerValue = if (selectedTimer == 0) null else selectedTimer
                    updateRoomInFirebase(room, updatedName, updatedIp, timerValue)
                } else {
                    Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun updateRoomInFirebase(room: Room, newName: String, newIp: String, newTimer: Int?) {
        val roomRef = database.child(room.name)
        roomRef.removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                val newRoomRef = database.child(newName)
                val updatedRoom = room.copy(name = newName, ip = newIp, timer = newTimer)
                newRoomRef.setValue(updatedRoom).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "$newName başarıyla güncellendi.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Güncelleme sırasında hata oluştu: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Oda güncellenemedi: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmationDialog(room: Room, containerLayout: LinearLayout) {
        AlertDialog.Builder(requireContext())
            .setTitle("Odayı Sil")
            .setMessage("${room.name} adlı odayı silmek istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                deleteRoomFromFirebase(room, containerLayout)
            }
            .setNegativeButton("Hayır") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun deleteRoomFromFirebase(room: Room, containerLayout: LinearLayout) {
        // Silme işleminde selectedPath kullanılarak doğru path'e erişilir
        val roomRef = database.child(selectedPath).child(room.name)
        roomRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                rooms.remove(room) // Odayı listeden çıkar
                listRooms(containerLayout) // Listeyi yeniden oluştur
                Toast.makeText(requireContext(), "${room.name} başarıyla silindi.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Silme işlemi başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        database.removeEventListener(valueEventListener)
    }
}
