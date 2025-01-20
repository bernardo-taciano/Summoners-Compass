package com.example.summonerscompass.presentation.crafting_screen

import ItemResponse
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.summonerscompass.network.DataDragonApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

class CraftingScreenViewModel: ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance("https://summoners-compass-default-rtdb.europe-west1.firebasedatabase.app")

    private val uid = auth.uid

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


}
