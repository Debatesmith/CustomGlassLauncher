package com.example.glasslauncher

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import androidx.core.graphics.drawable.toBitmap

data class AppBlock(
    val appName: String,
    val icon: Drawable,
    val packageName: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readParcelable<Bitmap>(Bitmap::class.java.classLoader)!!.let { BitmapDrawable(null, it) },
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(appName)
        parcel.writeParcelable(icon.toBitmap(), flags)
        parcel.writeString(packageName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AppBlock> {
        override fun createFromParcel(parcel: Parcel): AppBlock {
            return AppBlock(parcel)
        }

        override fun newArray(size: Int): Array<AppBlock?> {
            return arrayOfNulls(size)
        }
    }
}