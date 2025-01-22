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
import com.example.summonerscompass.models.EnergyPool
import com.example.summonerscompass.models.RandomSprite
import com.example.summonerscompass.network.DataDragonApi
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.ResponseBody
import kotlin.random.Random

class HomeScreenViewModel(
    private val locationManager: LocationManager
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance("https://summoners-compass-default-rtdb.europe-west1.firebasedatabase.app")

    private val uid = auth.uid

    private val _userPower = MutableStateFlow(0)
    val userPower: StateFlow<Int> = _userPower

    private val _energyPools = MutableStateFlow<List<EnergyPool>>(emptyList())
    val energyPools: StateFlow<List<EnergyPool>> = _energyPools

    private val _champions = MutableStateFlow<ChampionResponse?>(null)
    val champions: StateFlow<ChampionResponse?> = _champions

    private val _championSquare = MutableStateFlow<Bitmap?>(null)
    val championSquare: StateFlow<Bitmap?> = _championSquare

    private val _randomSprites = MutableStateFlow<List<RandomSprite>>(emptyList())
    val randomSprites: StateFlow<List<RandomSprite>> = _randomSprites

    private val _userLocation = MutableStateFlow(LatLng(0.0, 0.0))
    val userLocation: StateFlow<LatLng> = _userLocation

    private val _pinLocation = MutableStateFlow<LatLng?>(null)
    val pinLocation: StateFlow<LatLng?> = _pinLocation

    private var lastKnownRealLocation: LatLng = LatLng(38.7169, -9.1399)

    private val _userBearing = MutableStateFlow(0f)
    val userBearing: StateFlow<Float> = _userBearing

    init {
        startLocationUpdates()
        initialize()
    }

    private fun initialize() {
        fetchUserPower()
        startAutoSpriteGeneration()
        startAutoEnergyGeneration()
        monitorPlayerPosition()
    }


    fun monitorPlayerPosition() {
        viewModelScope.launch {
            userLocation.collect { currentLocation ->
                consumeEnergyPools(currentLocation, radius = 50f)
                consumeSprites(currentLocation, radius = 50f)
            }
        }
    }

    private fun startAutoEnergyGeneration() {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L) // 5 minutes in milliseconds
                generateRandomEnergyPools(5)
            }
        }
    }

    private fun startAutoSpriteGeneration() {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L) // 5 minutes in milliseconds
                generateRandomSprites(5)
            }
        }
    }

    fun generateRandomEnergyPools(count: Int) {
        viewModelScope.launch {
            val pools = mutableListOf<EnergyPool>()
            val baseLat = _userLocation.value.latitude
            val baseLng = _userLocation.value.longitude

            for (i in 1..count) {
                val randomLat = baseLat + Random.nextDouble(-0.01, 0.01)
                val randomLng = baseLng + Random.nextDouble(-0.01, 0.01)
                val randomPosition = LatLng(randomLat, randomLng)

                val powerValue = Random.nextInt(5, 20) // Valor de energia aleatório entre 5 e 20
                pools.add(EnergyPool(randomPosition, powerValue))
            }

            _energyPools.emit(pools)
        }
    }

    private fun LatLng.distanceTo(target: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(latitude, longitude, target.latitude, target.longitude, results)
        return results[0]
    }

    fun consumeEnergyPools(userLocation: LatLng, radius: Float) {
        viewModelScope.launch {
            val updatedPools = _energyPools.value.filterNot { pool ->
                if (userLocation.distanceTo(pool.position) <= radius) {
                    addPowerToUser(pool.powerValue) // Adiciona poder
                    true // Remove o pool
                } else {
                    false
                }
            }
            _energyPools.emit(updatedPools)
        }
    }

    private fun addPowerToUser(power: Int) {
        viewModelScope.launch {
            uid?.let {
                val userRef = db.reference.child("users").child(it).child("power")
                val currentPower = userRef.get().await().getValue(Int::class.java) ?: 0
                val updatedPower = currentPower + power
                userRef.setValue(updatedPower).addOnSuccessListener {
                    _userPower.value = updatedPower // Atualiza localmente
                }
            }
        }
    }


    fun fetchUserPower() {
        uid?.let {
            val powerRef = db.reference.child("users").child(it).child("power")
            powerRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentPower = snapshot.getValue(Int::class.java) ?: 0
                    _userPower.value = currentPower
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Failed to fetch user power: ${error.message}")
                }
            })
        }
    }

    fun updateUserPower(newPower: Int) {
        viewModelScope.launch {
            uid?.let {
                val powerRef = db.reference.child("users").child(it).child("power")
                powerRef.setValue(newPower).addOnSuccessListener {
                    _userPower.value = newPower
                }
            }
        }
    }

    fun consumeSprites(userLocation: LatLng, radius: Float) {
        viewModelScope.launch {
            uid?.let { uid ->
                val snapshot = db.reference.child("users").child(uid).child("glossary").get().await()
                val found = snapshot.children.mapNotNull { it.getValue(String::class.java) }

                val updatedSprites = _randomSprites.value.filterNot { sprite ->
                    val distance = userLocation.distanceTo(sprite.position)
                    if (distance <= radius && sprite.name !in found) {
                        saveSpriteToGlossary(sprite)
                        true
                    } else {
                        false
                    }
                }

                _randomSprites.emit(updatedSprites)
            }
        }
    }


    private fun saveSpriteToGlossary(sprite: RandomSprite) {
        viewModelScope.launch {
            uid?.let {
                val ref = db.reference.child("users").child(it).child("glossary").push()
                ref.setValue(sprite.name)
            }
        }
    }

    fun startLocationUpdates() {
        viewModelScope.launch {
            locationManager.getLocationUpdates().collect { (location, bearing) ->
                _userLocation.value = location
                _userBearing.value = bearing
                if (!locationManager.isTeleported) {
                    lastKnownRealLocation = location
                }
                // Generate initial energy pools and sprites when a valid location is received
                if (location.latitude != 0.0 && location.longitude != 0.0) {
                    if (_energyPools.value.isEmpty()) {
                        generateRandomEnergyPools(5)
                    }
                    if (_randomSprites.value.isEmpty()) {
                        generateRandomSprites(5)
                    }
                }
            }
        }
    }

    fun updatePinLocation(newLocation: LatLng) {
        _pinLocation.value = newLocation
    }

    fun teleportTo(newLocation: LatLng) {
        locationManager.setLocationOffset(newLocation, lastKnownRealLocation)
        _userLocation.value = newLocation
        _pinLocation.value = null
    }

    fun resetLocation() {
        locationManager.resetOffset()
        _userLocation.value = lastKnownRealLocation
        _pinLocation.value = null
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
            val baseLat = _userLocation.value.latitude
            val baseLng = _userLocation.value.longitude

            val champions = DataDragonApi.retrofitService.getChampions().data.values.shuffled().take(count)

            val sprites = champions.mapNotNull { champion ->
                try {
                    val randomLat = baseLat + Random.nextDouble(-0.01, 0.01)
                    val randomLng = baseLng + Random.nextDouble(-0.01, 0.01)
                    val randomPosition = LatLng(randomLat, randomLng)

                    val responseBody = DataDragonApi.retrofitService.getChampionSquare(champion.image.full)
                    val bitmap = BitmapFactory.decodeStream(responseBody.byteStream())
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 60, 60, true)

                    RandomSprite(randomPosition, champion.name, scaledBitmap)
                } catch (e: Exception) {
                    println("Error fetching sprite for ${champion.name}: ${e.message}")
                    null
                }
            }
            _randomSprites.emit(sprites)
        }
    }
}
