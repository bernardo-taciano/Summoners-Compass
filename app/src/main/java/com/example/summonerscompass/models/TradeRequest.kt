package com.example.summonerscompass.models

import Item
import com.google.android.gms.maps.model.LatLng

data class TradeRequest(
    val sender: User,
    val sendingItem: Item,
    val receivingItem: Item,
    val location: LatLng,
    val date: String,
    val time: String
)
