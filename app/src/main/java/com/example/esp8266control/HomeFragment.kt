package com.example.esp8266control

import Room
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private var containerLayout: LinearLayout? = null
    private val rooms = mutableListOf<Room>()
    private lateinit var database: DatabaseReference
    private lateinit var loggedInEmail: String

    companion object {
        fun newInstance(email: String): HomeFragment {
            val fragment = HomeFragment()
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
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        containerLayout = view.findViewById(R.id.roomContainer)

        val logoutButton = view.findViewById<Button>(R.id.btnLogout)
        logoutButton.setOnClickListener {
            (activity as MainActivity).logout() // MainActivity'deki logout işlevini çağır
        }

        database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("rooms")

        loadRoomsFromFirebase()

        return view
    }

    private fun loadRoomsFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rooms.clear()
                for (roomSnapshot in snapshot.children) {
                    val name = roomSnapshot.child("name").getValue(String::class.java) ?: ""
                    val ip = roomSnapshot.child("ip").getValue(String::class.java) ?: ""
                    val visible = roomSnapshot.child("visible").getValue(Boolean::class.java) ?: true
                    rooms.add(Room(name, ip, visible))
                }
                displayRooms()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Veriler yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayRooms() {
        containerLayout?.removeAllViews()

        for (room in rooms) {
            // Admin giriş yapmışsa tüm odalar gösterilir, değilse sadece "visible" olanlar
            if (loggedInEmail == "ckazanoglu@gmail.com" || room.visible) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(8, 8, 8, 8)
                }

                val button = Button(requireContext()).apply {
                    text = room.name

                    // Gradient arka planı uygula
                    setBackgroundResource(R.drawable.gradient_button)

                    // Yazı rengini siyah yap
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))

                    // Boyut ve stil ayarları
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        150 // Buton yüksekliği (px cinsinden)
                    )
                    layoutParams.setMargins(16, 16, 16, 16) // Kenar boşlukları
                    this.layoutParams = layoutParams
                    textSize = 18f // Yazı boyutu
                    setPadding(16, 16, 16, 16) // İçerik boşluğu
                }

                // Buton tıklama işlevi
                button.setOnClickListener {
                    Toast.makeText(requireContext(), "${room.name} butonuna tıklandı.", Toast.LENGTH_SHORT).show()
                }

                row.addView(button)
                containerLayout?.addView(row)
            }
        }
    }



}
