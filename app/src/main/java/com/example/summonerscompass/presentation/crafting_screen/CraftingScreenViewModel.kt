package com.example.summonerscompass.presentation.crafting_screen

import Item
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.summonerscompass.models.Trade
import com.example.summonerscompass.models.User
import com.example.summonerscompass.network.DataDragonApi
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
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

    private val _requests = MutableStateFlow<List<Trade>>(emptyList())
    val requests : StateFlow<List<Trade>> = _requests

    private val _trades = MutableStateFlow<List<Trade>>(emptyList())
    val trades : StateFlow<List<Trade>> = _trades

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val itemCombinations = mapOf(
        BF to mapOf(BF to DBLADE, ROD to GUNBLADE, BELT to STERAKS),
        ROD to mapOf(ROD to DCAP, BELT to RYLAIS),
        BELT to mapOf(BELT to WARMOG)
    )

    init {
        getInventory()
        getFriends()
        getTradeRequests()
    }

    fun getInventory() {
        _isLoading.value = true
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
                        _isLoading.value = false
                    }

                    override fun onCancelled(error: DatabaseError) {
                        println("Error accessing Firebase: ${error.message}")
                        _isLoading.value = false
                    }
                })
        } ?: run {
            println("User not authenticated.")
        }
    }

    fun getInventoryByEmail(email: String, onInventoryReceived: (List<Item?>) -> Unit) {
        viewModelScope.launch {
            try {
                val inv = ArrayList<Item?>()
                val userSnapshot = db.reference.child("users")
                    .orderByChild("email")
                    .equalTo(email)
                    .get()
                    .await()

                val userNode = userSnapshot.children.firstOrNull() ?: return@launch

                val inventoryItems = userNode.child("inventory").children.mapNotNull {
                    it.key
                }

                val itemResponse = DataDragonApi.retrofitService.getItems()
                for (id in inventoryItems) {
                    try {
                        val item = itemResponse.data[id]
                        if (item != null) {
                            inv.add(item)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                onInventoryReceived(inv)
            } catch (e: Exception) {
                e.printStackTrace()
                onInventoryReceived(emptyList())
            }
        }
    }

    fun sendTradeRequest(friendEmail: String, senderItemId: String, receiverItemId: String,
                         tradeLocation: LatLng, tradeDate: String, tradeTime: String) {
        viewModelScope.launch {
            uid?.let{
                val friendSnapshot = db.reference.child("users")
                    .orderByChild("email")
                    .equalTo(friendEmail)
                    .get()
                    .await()

                val friendNode = friendSnapshot.children.firstOrNull() ?: return@launch

                val fid = friendNode.key
                val tradeMap = mapOf(
                    "send" to receiverItemId,
                    "get" to senderItemId,
                    "lat" to tradeLocation.latitude,
                    "lng" to tradeLocation.longitude,
                    "date" to tradeDate,
                    "time" to tradeTime
                )

                fid?.let {
                    db.reference.child("trade_requests").child(fid).child(uid).setValue(tradeMap)
                }
            }
        }
    }

    private fun getInventoryItems(inventory: List<Pair<String, Int>>) {
        viewModelScope.launch(Dispatchers.IO) {
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
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 480, true)
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromBitmap(resizedBitmap, 0)

        viewModelScope.launch {
            try {
                val labels = imageLabeler?.process(inputImage)?.await()
                labels?.forEach { label ->
                   onObjectDetected(label.text)
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

    data class TradeDetails(val sendingItemId: String, val receivingItemId: String, val lat: Double, val lng: Double,
                            val date: String, val time: String)

    fun getTradeRequests() {
        viewModelScope.launch {
            uid?.let {
                db.reference.child("trade_requests").child(uid)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                println("No trade requests.")
                                return
                            }

                            val tradeMap = HashMap<String, TradeDetails>()
                            snapshot.children.mapNotNull { requestSnapshot ->
                                val list = ArrayList<Any?>()
                                val senderId = requestSnapshot.key
                                senderId?.let {
                                    val sendingItemId =
                                        requestSnapshot.child("send").getValue(String::class.java)
                                    val receivingItemId =
                                        requestSnapshot.child("get").getValue(String::class.java)
                                    val lat = requestSnapshot.child("lat").getValue(Double::class.java)
                                    val lng = requestSnapshot.child("lng").getValue(Double::class.java)
                                    val date = requestSnapshot.child("date").getValue(String::class.java)
                                    val time = requestSnapshot.child("time").getValue(String::class.java)
                                    if( sendingItemId != null && receivingItemId != null && lat != null && lng != null &&
                                        date != null && time != null) {
                                        val tradeDetails = TradeDetails(
                                            sendingItemId,
                                            receivingItemId,
                                            lat,
                                            lng,
                                            date,
                                            time
                                        )
                                        tradeMap[senderId] = tradeDetails
                                    }
                                }
                            }

                            getTradeRequestsData(tradeMap)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            println("Error accessing Firebase: ${error.message}")
                        }
                    })
            }
        }
    }

    private fun getTradeRequestsData(tradeMap: HashMap<String, TradeDetails>) {
        viewModelScope.launch(Dispatchers.IO){
            val reqs = ArrayList<Trade>()
            val usersRef = db.reference.child("users")
            val itemResponse = DataDragonApi.retrofitService.getItems()

            for((user, details) in tradeMap) {
                try {
                    val snapshot = usersRef.child(user).get().await()
                    val name = snapshot.child("name").getValue(String::class.java)
                    val email = snapshot.child("email").getValue(String::class.java)
                    val power = snapshot.child("power").getValue(Int::class.java)
                    if (name != null && email != null && power != null) {
                        val user = User(name, email, power)
                        val sendingItemId = details.sendingItemId
                        val receivingItemId = details.receivingItemId
                        val lat = details.lat
                        val lng = details.lng
                        val date = details.date
                        val time = details.time
                        val sendingItem = itemResponse.data[sendingItemId]
                        val receivingItem = itemResponse.data[receivingItemId]
                        val location = LatLng(lat, lng)
                        if(sendingItem != null && receivingItem != null) {
                            val res1 = sendingItem.image.full
                            val responseBody1: ResponseBody =
                                DataDragonApi.retrofitService.getItemSquare(res1)

                            val inputStream1 = responseBody1.byteStream()
                            val bitmap1: Bitmap = BitmapFactory.decodeStream(inputStream1)

                            val res2 = receivingItem.image.full
                            val responseBody2: ResponseBody =
                                DataDragonApi.retrofitService.getItemSquare(res2)

                            val inputStream2 = responseBody2.byteStream()
                            val bitmap2: Bitmap = BitmapFactory.decodeStream(inputStream2)

                            val tradeRequest = Trade(
                                user,
                                sendingItem,
                                receivingItem,
                                location,
                                date,
                                time,
                                bitmap1,
                                bitmap2
                            )

                            reqs.add(tradeRequest)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _requests.value = reqs
        }
    }

    fun acceptTradeRequest(trade: Trade) {
        viewModelScope.launch(Dispatchers.IO) {
            uid?.let{
                val friendSnapshot = db.reference.child("users")
                    .orderByChild("email")
                    .equalTo(trade.sender.email)
                    .get()
                    .await()

                val friendNode = friendSnapshot.children.firstOrNull() ?: return@launch

                val fid = friendNode.key
                fid?.let{
                    db.reference.child("trade_requests").child(uid).child(fid).removeValue()
                    val receiverMap = mapOf(
                        "get" to trade.receivingItem.image.full.dropLast(4),
                        "send" to trade.sendingItem.image.full.dropLast(4),
                        "lat" to trade.location.latitude,
                        "lng" to trade.location.longitude,
                        "date" to trade.date,
                        "time" to trade.time
                    )

                    val senderMap = mapOf(
                        "get" to trade.sendingItem.image.full.dropLast(4),
                        "send" to trade.receivingItem.image.full.dropLast(4),
                        "lat" to trade.location.latitude,
                        "lng" to trade.location.longitude,
                        "date" to trade.date,
                        "time" to trade.time
                    )
                    db.reference.child("users").child(uid).child("trades").child(fid).setValue(receiverMap)
                    db.reference.child("users").child(fid).child("trades").child(uid).setValue(senderMap)
                }
            }
        }
    }

    fun rejectTradeRequest(trade: Trade) {
        viewModelScope.launch(Dispatchers.IO) {
            uid?.let{
                val friendSnapshot = db.reference.child("users")
                    .orderByChild("email")
                    .equalTo(trade.sender.email)
                    .get()
                    .await()

                val friendNode = friendSnapshot.children.firstOrNull() ?: return@launch

                val fid = friendNode.key
                fid?.let{
                    db.reference.child("trade_requests").child(uid).child(fid).removeValue()
                }
            }
        }
    }

    fun getTrades() {
        viewModelScope.launch {
            uid?.let {
                db.reference.child("users").child(uid).child("trades")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                println("No trade requests.")
                                return
                            }

                            val tradeMap = HashMap<String, TradeDetails>()
                            snapshot.children.mapNotNull { requestSnapshot ->
                                val senderId = requestSnapshot.key
                                senderId?.let {
                                    val sendingItemId =
                                        requestSnapshot.child("send").getValue(String::class.java)
                                    val receivingItemId =
                                        requestSnapshot.child("get").getValue(String::class.java)
                                    val lat = requestSnapshot.child("lat").getValue(Double::class.java)
                                    val lng = requestSnapshot.child("lng").getValue(Double::class.java)
                                    val date = requestSnapshot.child("date").getValue(String::class.java)
                                    val time = requestSnapshot.child("time").getValue(String::class.java)
                                    if( sendingItemId != null && receivingItemId != null && lat != null && lng != null &&
                                        date != null && time != null) {
                                        val tradeDetails = TradeDetails(
                                            sendingItemId,
                                            receivingItemId,
                                            lat,
                                            lng,
                                            date,
                                            time
                                        )
                                        tradeMap[senderId] = tradeDetails
                                    }
                                }
                            }

                            getTradesData(tradeMap)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            println("Error accessing Firebase: ${error.message}")
                        }
                    })
            }
        }
    }

    private fun getTradesData(tradeMap: HashMap<String, TradeDetails>) {
        viewModelScope.launch(Dispatchers.IO){
            val trades = ArrayList<Trade>()
            val usersRef = db.reference.child("users")
            val itemResponse = DataDragonApi.retrofitService.getItems()

            for((user, details) in tradeMap) {
                try {
                    val snapshot = usersRef.child(user).get().await()
                    val name = snapshot.child("name").getValue(String::class.java)
                    val email = snapshot.child("email").getValue(String::class.java)
                    val power = snapshot.child("power").getValue(Int::class.java)
                    if (name != null && email != null && power != null) {
                        val user = User(name, email, power)
                        val sendingItemId = details.sendingItemId
                        val receivingItemId = details.receivingItemId
                        val lat = details.lat
                        val lng = details.lng
                        val date = details.date
                        val time = details.time
                        val sendingItem = itemResponse.data[sendingItemId]
                        val receivingItem = itemResponse.data[receivingItemId]
                        val location = LatLng(lat, lng)
                        if(sendingItem != null && receivingItem != null) {
                            val res1 = sendingItem.image.full
                            val responseBody1: ResponseBody =
                                DataDragonApi.retrofitService.getItemSquare(res1)

                            val inputStream1 = responseBody1.byteStream()
                            val bitmap1: Bitmap = BitmapFactory.decodeStream(inputStream1)

                            val res2 = receivingItem.image.full
                            val responseBody2: ResponseBody =
                                DataDragonApi.retrofitService.getItemSquare(res2)

                            val inputStream2 = responseBody2.byteStream()
                            val bitmap2: Bitmap = BitmapFactory.decodeStream(inputStream2)

                            val tradeRequest = Trade(
                                user,
                                sendingItem,
                                receivingItem,
                                location,
                                date,
                                time,
                                bitmap1,
                                bitmap2
                            )

                            trades.add(tradeRequest)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _trades.value = trades
        }
    }

    fun confirmTrade(trade: Trade) {
        viewModelScope.launch(Dispatchers.IO) {
            uid?.let{
                val friendSnapshot = db.reference.child("users")
                    .orderByChild("email")
                    .equalTo(trade.sender.email)
                    .get()
                    .await()

                val friendNode = friendSnapshot.children.firstOrNull() ?: return@launch

                val fid = friendNode.key
                fid?.let{
                    db.reference.child("users").child(uid).child("trades").child(fid).removeValue()
                    db.reference.child("users").child(fid).child("trades").child(uid).removeValue()
                }
            }
        }
    }
}
