package com.example.summonerscompass.presentation.home_screen

import Champion
import ChampionResponse
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.summonerscompass.models.RandomSprite
import com.example.summonerscompass.network.DataDragonApi
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import kotlin.random.Random

class HomeScreenViewModel() : ViewModel() {
    private val _champions = MutableStateFlow<ChampionResponse?>(null)
    val champions: StateFlow<ChampionResponse?> = _champions

    private val _championSquare = MutableStateFlow<Bitmap?>(null)
    val championSquare: StateFlow<Bitmap?> = _championSquare

    private val _randomSprites = MutableStateFlow<List<RandomSprite>>(emptyList())
    val randomSprites: StateFlow<List<RandomSprite>> = _randomSprites

    fun getChampionSquare(res: String) {
        viewModelScope.launch {
            try {
                val responseBody: ResponseBody = DataDragonApi.retrofitService.getChampionSquare(res)

                // convert response into bitmap
                val inputStream = responseBody.byteStream()
                val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

                _championSquare.value = bitmap

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getChampions() {
        viewModelScope.launch {
            try {
                _champions.value = DataDragonApi.retrofitService.getChampions()
                println(_champions.value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateRandomSprites(count: Int) {
        viewModelScope.launch {
            val sprites = mutableListOf<RandomSprite>()
            val baseLat = 40.9971
            val baseLng = 29.1007

            val championData = DataDragonApi.retrofitService.getChampions().data

            repeat(count) {
                val randomLat = baseLat + Random.nextDouble(-0.05, 0.05)
                val randomLng = baseLng + Random.nextDouble(-0.05, 0.05)
                val randomPosition = LatLng(randomLat, randomLng)

                val randomChampionKey = championData.keys.randomOrNull()
                val randomChampion = randomChampionKey?.let { championData[it] }

                if (randomChampion != null) {
                    val res = randomChampion.image.full // filename
                    val name = randomChampion.name
                    try {
                        val responseBody = DataDragonApi.retrofitService.getChampionSquare(res)
                        val inputStream = responseBody.byteStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        val randomSprite = RandomSprite(randomPosition, name, bitmap)
                        sprites.add(randomSprite)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            _randomSprites.value = sprites
        }
    }

}
