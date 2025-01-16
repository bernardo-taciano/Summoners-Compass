package com.example.summonerscompass.presentation.home_screen

import Champion
import ChampionResponse
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.summonerscompass.models.RandomSprite
import com.example.summonerscompass.network.DataDragonApi
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import kotlin.random.Random

class HomeScreenViewModel() : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance("https://summoners-compass-default-rtdb.europe-west1.firebasedatabase.app")

    private val uid = auth.uid

    private val _champions = MutableStateFlow<ChampionResponse?>(null)
    val champions: StateFlow<ChampionResponse?> = _champions

    private val _championSquare = MutableStateFlow<Bitmap?>(null)
    val championSquare: StateFlow<Bitmap?> = _championSquare

    private val _randomSprites = MutableStateFlow<List<RandomSprite>>(emptyList())
    val randomSprites: StateFlow<List<RandomSprite>> = _randomSprites

    private val _userLocation = MutableStateFlow(LatLng(38.7169, -9.1399)) // Default: Lisbon
    val userLocation: StateFlow<LatLng> = _userLocation

    private val _pinLocation = MutableStateFlow<LatLng?>(null)
    val pinLocation: StateFlow<LatLng?> = _pinLocation

    init {
        startAutoSpriteGeneration()
    }

    private fun startAutoSpriteGeneration() {
        viewModelScope.launch {
            while (true) {
                generateRandomSprites(5)
                delay(5 * 60 * 1000L) // 5 minutes in milliseconds
            }
        }
    }

    fun consumeSprites(userLocation: LatLng, radius: Float) {
        viewModelScope.launch {
            for(sprite in _randomSprites.value) {
                val distance = FloatArray(1) // para guardar o resultado do calculo da distancia
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    sprite.position.latitude, sprite.position.longitude,
                    distance
                )

                if(distance[0] <= radius) {
                    saveSpriteToGlossary(sprite)
                }
            }
        }
    }

    private fun saveSpriteToGlossary(sprite: RandomSprite) {
        viewModelScope.launch {
            val champion = DataDragonApi.retrofitService.getChampion(sprite.name)
            uid?.let {
                val ref = db.reference.child("users").child(it).child("glossary").push()
                ref.setValue(sprite.name)
            }
        }
    }

    fun updatePinLocation(newLocation: LatLng) {
        _pinLocation.value = newLocation
        _userLocation.value = newLocation // Update user location for teleportation
    }

    fun teleportTo(newLocation: LatLng) {
        _userLocation.value = newLocation
    }

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
            val baseLat = _userLocation.value.latitude
            val baseLng = _userLocation.value.longitude

            val championDataList = DataDragonApi.retrofitService.getChampions().data.values.toList()
            val championDataShuffled = championDataList.shuffled().take(count)

            for(champion in championDataShuffled) {
                val randomLat = baseLat + Random.nextDouble(-0.01, 0.01)
                val randomLng = baseLng + Random.nextDouble(-0.01, 0.01)
                val randomPosition = LatLng(randomLat, randomLng)

                val res = champion.image.full // filename
                val name = champion.name
                try {
                    val responseBody = DataDragonApi.retrofitService.getChampionSquare(res)
                    val inputStream = responseBody.byteStream()
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)

                    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 60, 60, true)

                    val randomSprite = RandomSprite(randomPosition, name, scaledBitmap)
                    sprites.add(randomSprite)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            _randomSprites.emit(sprites)
        }
    }

}
