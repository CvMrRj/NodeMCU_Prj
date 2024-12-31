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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.*
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var containerLayout: LinearLayout? = null
    private val rooms = mutableListOf<Room>()
    private lateinit var database: DatabaseReference
    private lateinit var loggedInEmail: String
    private lateinit var valueEventListener: ValueEventListener

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
            (activity as MainActivity).logout()
        }

        database = FirebaseDatabase.getInstance("https://esp8266-617c1-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("rooms")

        setupFirebaseListener()
        return view
    }

    private fun setupFirebaseListener() {
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rooms.clear()
                for (roomSnapshot in snapshot.children) {
                    val name = roomSnapshot.child("name").getValue(String::class.java) ?: ""
                    val ip = roomSnapshot.child("ip").getValue(String::class.java) ?: ""
                    val visible = roomSnapshot.child("visible").getValue(Boolean::class.java) ?: true
                    rooms.add(Room(name, ip, visible))
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

        for (room in rooms) {
            if (loggedInEmail == "ckazanoglu@gmail.com" || room.visible) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(8, 8, 8, 8)
                }

                val button = Button(requireContext()).apply {
                    text = room.name

                    val background = ContextCompat.getDrawable(requireContext(), R.drawable.button_animation) as LayerDrawable
                    background?.let { setBackground(it) }

                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))

                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        150
                    )
                    layoutParams.setMargins(16, 16, 16, 16)
                    this.layoutParams = layoutParams
                    textSize = 18f
                    setPadding(16, 16, 16, 16)

                    val greenOverlay = background.findDrawableByLayerId(R.id.greenOverlay) as GradientDrawable
                    greenOverlay.alpha = 0
                }

                button.setOnClickListener {
                    startButtonAnimation(button)
                    updateFirebaseState(room.name)
                    Toast.makeText(requireContext(), "${room.name} butonuna tıklandı.", Toast.LENGTH_SHORT).show()
                }

                row.addView(button)
                containerLayout?.addView(row)
            }
        }
    }

    private fun updateFirebaseState(roomName: String) {
        val stateRef = database.child(roomName).child("state")

        stateRef.setValue(1).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "State güncellendi: $roomName -> 1", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "State güncellenemedi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startButtonAnimation(button: Button) {
        val originalBackground = button.background
        val greenBackground = GradientDrawable().apply {
            setColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            cornerRadius = 16f
        }

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000 // 1 saniyede yeşile geçiş
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                val color = blendColors(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_light),
                    ContextCompat.getColor(requireContext(), android.R.color.white),
                    fraction
                )
                button.setBackgroundColor(color)
            }
            doOnEnd {
                button.postDelayed({
                    button.background = originalBackground
                }, 3000) // 3 saniye sonra orijinal arka plana dön
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
