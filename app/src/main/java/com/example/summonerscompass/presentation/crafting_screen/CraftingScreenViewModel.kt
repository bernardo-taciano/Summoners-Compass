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
import com.example.summonerscompass.models.User
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

private const val BF = "1038"
private const val ROD = "1058"
private const val BELT = "1011"
private const val GUNBLADE = "223146"
private const val STERAKS = "223053"
private const val RYLAIS = "223116"
private const val WARMOG = "3083"
private const val DCAP = "223089"
private const val DBLADE = "228003"

class CraftingScreenViewModel(): ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance("https://summoners-compass-default-rtdb.europe-west1.firebasedatabase.app")

    private val uid = auth.uid

    var imageLabeler: ImageLabeler? = null

    private val _inventory = MutableStateFlow<List<Pair<Item?, Int>>>(emptyList())
    val inventory: StateFlow<List<Pair<Item?, Int>>> = _inventory

    private val _squares = MutableStateFlow<List<Bitmap>>(emptyList())
    val squares: StateFlow<List<Bitmap>> = _squares

    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends : StateFlow<List<User>> = _friends

    private val itemCombinations = mapOf(
        BF to mapOf(BF to DBLADE, ROD to GUNBLADE, BELT to STERAKS),
        ROD to mapOf(ROD to DCAP, BELT to RYLAIS),
        BELT to mapOf(BELT to WARMOG)
    )

    init {
        getInventory()
    }

    fun getInventory() {
        uid?.let { uid ->
            db.reference.child("users").child(uid).child("inventory")
                .addValueEventListener(object : ValueEventListener {
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

    fun combineItems(item1: String, item2: String) {
        uid?.let { uid ->
            val userInventoryRef = db.reference.child("users").child(uid).child("inventory")

            userInventoryRef.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    println("Empty inventory.")
                    return@addOnSuccessListener
                }

                val inventoryMap = snapshot.children.associate {
                    ((it.key to it.child("count").getValue(Int::class.java))
                        ?: 0) as Pair<String, Int>
                }

                // Check if both items are in inventory
                if (inventoryMap[item1] == null || inventoryMap[item1]!! <= 0 ||
                    inventoryMap[item2] == null || inventoryMap[item2]!! <= 0 ||
                    (item1 == item2 && inventoryMap[item1]!! < 2)
                ) {
                    println("One or both items are not available in the inventory.")
                    return@addOnSuccessListener
                }

                // Get the combined item
                val combinedItem = itemCombinations[item1]?.get(item2) ?: itemCombinations[item2]?.get(item1)
                if (combinedItem == null) {
                    println("These items cannot be combined.")
                    return@addOnSuccessListener
                }

                // Update inventory: Remove the two items
                val updates = mutableMapOf<String, Any?>()
                if (item1 == item2){
                    if (inventoryMap[item1] == 2) updates["$item1/count"] =
                        null else updates["$item1/count"] = inventoryMap[item1]!! - 2
                } else {
                    if (inventoryMap[item1] == 1) updates["$item1/count"] =
                        null else updates["$item1/count"] = inventoryMap[item1]!! - 1
                    if (inventoryMap[item2] == 1) updates["$item2/count"] =
                        null else updates["$item2/count"] = inventoryMap[item2]!! - 1
                }

                // Add the combined item
                if (inventoryMap[combinedItem] == null) {
                    updates["$combinedItem/count"] = 1
                } else {
                    updates["$combinedItem/count"] = inventoryMap[combinedItem]!! + 1
                }

                // Apply updates to the database
                userInventoryRef.updateChildren(updates)
                    .addOnSuccessListener {
                        println("Successfully combined $item1 and $item2 into $combinedItem.")
                    }
                    .addOnFailureListener { e ->
                        println("Failed to update inventory: ${e.message}")
                    }
            }.addOnFailureListener { e ->
                println("Error accessing inventory: ${e.message}")
            }
        } ?: run {
            println("User not authenticated.")
        }
    }

    fun getFriends() {
        viewModelScope.launch {
            uid?.let {
                db.reference.child("users").child(uid).child("friends")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                println("Empty friends list.")
                                return
                            }

                            val friendsList = snapshot.children.mapNotNull { friendSnapshot ->
                                val friendId = friendSnapshot.key
                                friendId
                            }

                            getFriendsData(friendsList)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            println("Error accessing Firebase: ${error.message}")
                        }
                    })
            }
        }
    }

    private fun getFriendsData(friendsList: List<String>) {
        viewModelScope.launch {
            try{
                val friends = ArrayList<User>()
                val usersRef = db.reference.child("users")
                for (fid in friendsList) {
                    try {
                        val snapshot = usersRef.child(fid).get().await()
                        val name = snapshot.child("name").getValue(String::class.java)
                        val email = snapshot.child("email").getValue(String::class.java)
                        //val profileImage = snapshot.child("profileImage").getValue(String::class.java)
                        val power = snapshot.child("power").getValue(Int::class.java)
                        if (name != null && email != null && power != null) {
                            val user = User(name, email, power)
                            friends.add(user)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                _friends.value = friends
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
