package com.example.summonerscompass.presentation.crafting_screen

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
import com.google.firebase.database.FirebaseDatabase
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

    private val _items = MutableStateFlow<ItemResponse?>(null)
    val items: StateFlow<ItemResponse?> = _items

    private val _itemSquare = MutableStateFlow<Bitmap?>(null)
    val itemSquare: StateFlow<Bitmap?> = _itemSquare

    fun getItems() {
        viewModelScope.launch {
            try {
                _items.value = DataDragonApi.retrofitService.getItems()
                println(_items.value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getItemSquare(res: String) {
        viewModelScope.launch {
            try {
                val responseBody: ResponseBody = DataDragonApi.retrofitService.getItemSquare(res)

                // convert response into bitmap
                val inputStream = responseBody.byteStream()
                val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

                _itemSquare.value = bitmap

            } catch (e: Exception) {
                e.printStackTrace()
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
                    if (label.text == "knife") {
                        onObjectDetected("knife")
                    } else if (label.text == "fork") {
                        onObjectDetected("fork")
                    } else if (label.text == "spoon") {
                        onObjectDetected("spoon")
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
                val userInventoryRef = db.getReference("users").child(userId).child("inventory")

                userInventoryRef.orderByKey().equalTo(itemId).get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            snapshot.children.forEach { childSnapshot ->
                                val currentCount =
                                    childSnapshot.child("count").getValue(Int::class.java) ?: 0
                                childSnapshot.ref.child("count").setValue(currentCount + 1)
                            }
                        } else {
                            val newItem = mapOf(
                                "itemId" to itemId,
                                "count" to 1
                            )
                            userInventoryRef.push().setValue(newItem)
                        }
                    }.addOnFailureListener { e ->
                    e.printStackTrace()
                }
            }
        }
    }
}
