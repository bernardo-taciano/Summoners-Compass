package com.example.summonerscompass.presentation.profile_screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.summonerscompass.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendsScreenViewModel() : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance("https://summoners-compass-default-rtdb.europe-west1.firebasedatabase.app")

    private val uid = auth.uid

    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends : StateFlow<List<User>> = _friends

    private val _requests = MutableStateFlow<List<User>>(emptyList())
    val requests : StateFlow<List<User>> = _requests

    init {
        getFriends()
        getRequests()
    }

    fun getRequests() {
        viewModelScope.launch {
            uid?.let {
                db.reference.child("friend_requests").child(uid)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                println("No friend requests.")
                                return
                            }

                            val requestList = snapshot.children.mapNotNull { requestSnapshot ->
                                val senderId = requestSnapshot.key
                                senderId
                            }

                            getRequestsData(requestList)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            println("Error accessing Firebase: ${error.message}")
                        }
                    })
            }
        }
    }

    private fun getRequestsData(requestsList: List<String>) {
        viewModelScope.launch {
            try{
                val requests = ArrayList<User>()
                val usersRef = db.reference.child("users")
                for (sid in requestsList) {
                    try {
                        val snapshot = usersRef.child(sid).get().await()
                        val name = snapshot.child("name").getValue(String::class.java)
                        val email = snapshot.child("email").getValue(String::class.java)
                        //val profileImage = snapshot.child("profileImage").getValue(String::class.java)
                        val power = snapshot.child("power").getValue(Int::class.java)
                        if (name != null && email != null && power != null) {
                            val user = User(name, email, power)
                            requests.add(user)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                _requests.value = requests
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendFriendRequest(email: String) {
        db.reference.child("users").orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val rid = userSnapshot.key
                            var areFriends = false

                            for (user in _friends.value){
                                if (user.email == email) {
                                    areFriends = true
                                }
                            }

                            if (rid != null && uid != null && !areFriends) {
                                db.reference.child("friend_requests").child(rid).child(uid).setValue(true)
                            }
                        }
                    } else {
                        Log.d("UserSearch", "No user found with email: $email")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserSearch", "Error searching for user: ${error.message}")
                }
            })
    }

    fun removeFriend(email: String) {
        viewModelScope.launch {
            uid?.let {
                db.reference.child("users").orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (userSnapshot in snapshot.children) {
                                    val friendId = userSnapshot.key
                                    if (friendId != null) {
                                        db.reference.child("users").child(uid).child("friends").child(friendId).removeValue()
                                        db.reference.child("users").child(friendId).child("friends").child(uid).removeValue()

                                        val updatedFriends = _friends.value.filter { it.email != email }
                                        _friends.value = updatedFriends
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("UserSearch", "Error removing friend: ${error.message}")
                        }
                    })
            }
        }
    }


    fun acceptFriendRequest(email: String) {
        viewModelScope.launch {
            uid?.let {
                db.reference.child("users").orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (userSnapshot in snapshot.children) {
                                    val sid = userSnapshot.key
                                    if (sid != null) {
                                        db.reference.child("friend_requests").child(uid).child(sid).removeValue()

                                        db.reference.child("users").child(uid).child("friends").child(sid).setValue(true)
                                        db.reference.child("users").child(sid).child("friends").child(uid).setValue(true)

                                        val updatedRequests = _requests.value.filter { it.email != email }
                                        _requests.value = updatedRequests

                                        val name = userSnapshot.child("name").getValue(String::class.java)
                                        val power = userSnapshot.child("power").getValue(Int::class.java)
                                        if (name != null && power != null) {
                                            val newFriend = User(name, email, power)
                                            val updatedFriends = _friends.value.toMutableList()
                                            updatedFriends.add(newFriend)
                                            _friends.value = updatedFriends
                                        }
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("UserSearch", "Error searching for user: ${error.message}")
                        }
                    })
            }
        }
    }


    fun rejectFriendRequest(email: String) {
        viewModelScope.launch {
            uid?.let {
                db.reference.child("users").orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (userSnapshot in snapshot.children) {
                                    val sid = userSnapshot.key
                                    if (sid != null) {
                                        db.reference.child("friend_requests").child(uid).child(sid).removeValue()

                                        val updatedRequests = _requests.value.filter { it.email != email }
                                        _requests.value = updatedRequests
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("UserSearch", "Error searching for user: ${error.message}")
                        }
                    })
            }
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