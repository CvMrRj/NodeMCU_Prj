import android.os.Parcel
import android.os.Parcelable

data class Room(
    val name: String,
    val ip: String,
    var visible: Boolean,
    val timer: Int? = null, // Timer alanı
    val role: String? = null // Role alanı
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readValue(Int::class.java.classLoader) as? Int, // Timer okunuyor
        parcel.readString() // Role okunuyor
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(ip)
        parcel.writeByte(if (visible) 1 else 0)
        parcel.writeValue(timer) // Timer yazılıyor
        parcel.writeString(role) // Role yazılıyor
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Room> {
        override fun createFromParcel(parcel: Parcel): Room = Room(parcel)
        override fun newArray(size: Int): Array<Room?> = arrayOfNulls(size)
    }
}
