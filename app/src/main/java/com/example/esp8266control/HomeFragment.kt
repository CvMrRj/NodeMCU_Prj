package com.example.esp8266control

import Room
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.animation.doOnEnd
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
            if (loggedInEmail == "ckazanoglu@gmail.com" || room.visible) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(8, 8, 8, 8)
                }

                val button = Button(requireContext()).apply {
                    text = room.name

                    // Gradient arka planı uygula
                    val background = ContextCompat.getDrawable(requireContext(), R.drawable.button_animation) as LayerDrawable
                    background?.let { setBackground(it) }

                    // Yazı rengini siyah yap
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))

                    // Boyut ve stil ayarları
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        150 // Buton yüksekliği
                    )
                    layoutParams.setMargins(16, 16, 16, 16)
                    this.layoutParams = layoutParams
                    textSize = 18f
                    setPadding(16, 16, 16, 16)


                    // Başlangıçta yeşil tabakayı tamamen şeffaf yap
                    val greenOverlay = background.findDrawableByLayerId(R.id.greenOverlay) as GradientDrawable
                    greenOverlay.alpha = 0
                }

                // Buton tıklama işlevi
                button.setOnClickListener {
                    startButtonAnimation(button)
                    updateFirebaseState(room.name) // Firebase'deki state durumunu güncelle
                    Toast.makeText(requireContext(), "${room.name} butonuna tıklandı.", Toast.LENGTH_SHORT).show()
                }

                row.addView(button)
                containerLayout?.addView(row)
            }
        }
    }


    private fun updateFirebaseState(roomName: String) {
        val database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
        val stateRef = database.getReference("rooms/$roomName/state")

        // State'i 1 yap
        stateRef.setValue(1).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "State güncellendi: $roomName -> 1", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "State güncellenemedi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }





    private fun startButtonAnimation(button: Button) {
        val drawable = button.background as? LayerDrawable
        val greenOverlay = drawable?.findDrawableByLayerId(R.id.greenOverlay) as? GradientDrawable

        if (greenOverlay != null) {
            // Animasyon oluştur
            val animator = ValueAnimator.ofInt(0, 255).apply {
                duration = 1000 // 1 saniye
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Int
                    greenOverlay.alpha = value // Şeffaflıktan opaklığa geçiş
                    button.invalidate()
                }
                doOnEnd {
                    // Animasyon tamamlandığında, yeşil geçiş tekrar şeffaf olur
                    greenOverlay.alpha = 0
                    button.invalidate()
                }
            }

            // Animasyonu başlat
            animator.start()
        }
    }






}
