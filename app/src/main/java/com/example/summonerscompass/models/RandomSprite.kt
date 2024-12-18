package com.example.summonerscompass.models

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng

data class RandomSprite(
    val position: LatLng,
    val name: String,
    val image: Bitmap
)
