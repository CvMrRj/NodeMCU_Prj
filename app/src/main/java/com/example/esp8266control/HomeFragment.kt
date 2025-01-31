package com.example.esp8266control

import Room
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private var containerLayout: LinearLayout? = null
    private val rooms = mutableListOf<Room>()
    private lateinit var database: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        containerLayout = view.findViewById(R.id.roomContainer)
        loggedInEmail = arguments?.getString("email") ?: "default@example.com"
        val logoutButton = view.findViewById<Button>(R.id.btnLogout)

        logoutButton.setOnClickListener {
            (activity as MainActivity).logout()
        }

        // Firebase bağlantısı
        database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference

        setupFirebaseListener()

        return view
    }

    private fun setupFirebaseListener() {
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rooms.clear()

                val allRooms = listOf("Kart1", "Kart2")
                for (path in allRooms) {
                    val pathSnapshot = snapshot.child(path)
                    for (roomSnapshot in pathSnapshot.children) {
                        val name = roomSnapshot.child("name").getValue(String::class.java) ?: ""
                        val ip = roomSnapshot.child("ip").getValue(String::class.java) ?: ""
                        val visible = roomSnapshot.child("visible").getValue(Boolean::class.java) ?: true
                        val timer = roomSnapshot.child("timer").getValue(Int::class.java)
                        val role = roomSnapshot.child("role").getValue(String::class.java)

                        val room = Room(name, ip, visible, timer, role, path)
                        rooms.add(room)
                    }
                }

                if (isAdded && context != null) {
                    displayRooms()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Veriler yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        database.addValueEventListener(valueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        database.removeEventListener(valueEventListener)
    }

    private fun displayRooms() {
        containerLayout?.removeAllViews()

        val groupedRooms = rooms.groupBy { it.path } // Group rooms by path

        for ((group, roomList) in groupedRooms) {
            val groupTitle = TextView(requireContext()).apply {
                text = group.capitalize()
                textSize = 20f
                setPadding(16, 16, 16, 8)
            }
            containerLayout?.addView(groupTitle)

            for (room in roomList) {
                if (loggedInEmail == "ckazanoglu@gmail.com" || room.visible) {
                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(8, 8, 8, 8)
                    }

                    val button = createButton(room)
                    button.setOnClickListener {
                        startButtonAnimation(button, room.role == "reverse")
                        updateFirebaseState(room)
                    }

                    row.addView(button)
                    containerLayout?.addView(row)
                }
            }
        }
    }


    private fun createButton(room: Room): RelativeLayout {
        // Dış layout (butonun tamamı)
        val container = RelativeLayout(requireContext()).apply {
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                150
            ).apply {
                setMargins(16, 16, 16, 16)
            }
            this.layoutParams = layoutParams
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
        }

        // Oda adı text
        val textView = TextView(requireContext()).apply {
            id = View.generateViewId()
            text = room.name
            textSize = 18f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            val textParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_VERTICAL)
                addRule(RelativeLayout.ALIGN_PARENT_START)
                marginStart = 24
            }
            layoutParams = textParams
        }

        // Buton tıklama olayları
        container.setOnClickListener {
            startButtonAnimation(container, room.role == "reverse")
            updateFirebaseState(room)
        }

        // Durumuna göre arka plan rengi ayarla
        val stateRef = database.child(room.path).child(room.name).child("state")
        stateRef.get().addOnSuccessListener { snapshot ->
            val state = snapshot.getValue(Int::class.java) ?: 0
            val backgroundDrawable = container.background as GradientDrawable
            backgroundDrawable.setColor(
                if (state == 1) {
                    if (room.role == "reverse") {
                        ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    } else {
                        ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
                    }
                } else {
                    ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                }
            )
            container.invalidate()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Veri yüklenirken hata oluştu: ${it.message}", Toast.LENGTH_SHORT).show()
        }

        container.addView(textView)
        return container
    }




    private fun updateFirebaseState(room: Room) {
        val stateRef = database.child(room.path).child(room.name).child("state")
        val timerRef = room.timer ?: 0
        val role = room.role

        stateRef.get().addOnSuccessListener { snapshot ->
            val currentState = snapshot.getValue(Int::class.java) ?: 0
            val newState = if (role == "reverse") {
                if (currentState == 1) 0 else 1
            } else {
                if (currentState == 0) 1 else 0
            }

            stateRef.setValue(newState).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (newState == 1 && timerRef > 0) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            stateRef.setValue(0).addOnSuccessListener {
                                Log.d("TimerReset", "Timer tamamlandı: ${room.name}, state sıfırlandı.")
                            }.addOnFailureListener {
                                Log.e("TimerReset", "Timer sıfırlama başarısız: ${it.message}")
                            }
                        }, timerRef * 1000L)
                    }

                } else {
                    Toast.makeText(requireContext(), "State güncellenemedi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startButtonAnimation(view: View, isReverse: Boolean) {
        val buttonBackground = view.background as GradientDrawable
        val originalColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        val activeColor = if (isReverse) {
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        } else {
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
        }

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000 // 3 saniyelik bir animasyon
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                val blendedColor = blendColors(activeColor, originalColor, fraction)
                buttonBackground.setColor(blendedColor)
            }
            doOnEnd {
                buttonBackground.setColor(originalColor) // Animasyon bittikten sonra eski rengi uygular
            }
        }
        animator.start()
    }


    private fun blendColors(color1: Int, color2: Int, fraction: Float): Int {
        val red = (1 - fraction) * (color1 shr 16 and 0xff) + fraction * (color2 shr 16 and 0xff)
        val green = (1 - fraction) * (color1 shr 8 and 0xff) + fraction * (color2 shr 8 and 0xff)
        val blue = (1 - fraction) * (color1 and 0xff) + fraction * (color2 and 0xff)
        return (0xff shl 24) or ((red.toInt() and 0xff) shl 16) or ((green.toInt() and 0xff) shl 8) or (blue.toInt() and 0xff)
    }

}
