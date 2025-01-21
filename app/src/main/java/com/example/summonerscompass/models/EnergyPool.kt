package com.example.summonerscompass.models

import com.google.android.gms.maps.model.LatLng

data class EnergyPool(
    val position: LatLng,
    val powerValue: Int // Quantidade de energia que o jogador ganha ao consumir
)

