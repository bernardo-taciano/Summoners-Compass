package com.example.summonerscompass.presentation.crafting_screen

import Item
import ItemResponse
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.summonerscompass.network.DataDragonApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.ResponseBody


class CraftingScreenViewModel(): ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance("https://summoners-compass-default-rtdb.europe-west1.firebasedatabase.app")

    private val uid = auth.uid

    var cameraPermissionGranted by mutableStateOf(false)
    var cameraProvider: ProcessCameraProvider? = null
    var imageLabeler: ImageLabeler? = null

    private val _inventory = MutableStateFlow<List<Pair<Item?, Int>>>(emptyList())
    val inventory: StateFlow<List<Pair<Item?, Int>>> = _inventory

    private val _squares = MutableStateFlow<List<Bitmap>>(emptyList())
    val squares: StateFlow<List<Bitmap>> = _squares

    init {
        getInventory()
    }

    fun getInventory() {
        uid?.let { uid ->
            db.reference.child("users").child(uid).child("inventory")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            println("Empty inventory.")
                            return
                        }

                        val inventoryList = snapshot.children.mapNotNull { itemSnapshot ->
                            val itemId = itemSnapshot.key  //item key
                            val count = itemSnapshot.child("count").getValue(Int::class.java) // count
                            if (itemId != null && count != null) {
                                itemId to count  // pair (item,count)
                            } else {
                                null
                            }
                        }
                        getInventoryItems(inventoryList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        println("Error accessing Firebase: ${error.message}")
                    }
                })
        } ?: run {
            println("User not authenticated.")
        }
    }

    private fun getInventoryItems(inventory: List<Pair<String, Int>>) {
        viewModelScope.launch {
            try {
                val itemList = ArrayList<Pair<Item?, Int>>()
                val squareList = ArrayList<Bitmap>()
                val itemResponse = DataDragonApi.retrofitService.getItems()

                for ((id, count) in inventory) {
                    try {
                        val item = itemResponse.data[id]

                        if(item != null) {
                            val pair = Pair(item, count)
                            itemList.add(pair)

                            val res = item.image.full
                            val responseBody: ResponseBody =
                                DataDragonApi.retrofitService.getItemSquare(res)

                            val inputStream = responseBody.byteStream()
                            val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
                            squareList.add(bitmap)
                        } else {
                            println("Item not found: $id")
                        }
                    } catch (e: Exception) {
                        println("Error getting item $id: ${e.message}")
                    }
                }

                _squares.value = squareList
                _inventory.value = itemList
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error retrieving items: ${e.message}")
            }
        }
    }

    fun startObjectDetection(bitmap: Bitmap, onObjectDetected: (String) -> Unit) {
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        viewModelScope.launch {
            try {
                val labels = imageLabeler?.process(inputImage)?.await()
                labels?.forEach { label ->
                    if (label.text == "Ring") {
                        onObjectDetected("Ring")
                    } else if (label.text == "Cutlery") {
                        onObjectDetected("Cutlery")
                    } else if (label.text == "Glasses") {
                        onObjectDetected("Glasses")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addItemToInventory(itemId: String) {
        viewModelScope.launch {
            uid?.let { userId ->
                val userInventoryRef = db.getReference("users").child(userId).child("inventory").child(itemId)
                userInventoryRef.get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            val currentCount = snapshot.child("count").getValue(Int::class.java) ?: 0
                            userInventoryRef.child("count").setValue(currentCount + 1)
                        } else {
                            val newItem = mapOf(
                                "count" to 1
                            )
                            userInventoryRef.setValue(newItem)
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                    }
            }
        }
    }
}
