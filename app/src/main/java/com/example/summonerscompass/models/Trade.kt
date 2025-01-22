package com.example.summonerscompass.models

import Item
import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng

data class Trade(
    val sender: User,
    val sendingItem: Item,
    val receivingItem: Item,
    val location: LatLng,
    val date: String,
    val time: String,
    val sendingSquare: Bitmap,
    val receivingSquare: Bitmap
)
