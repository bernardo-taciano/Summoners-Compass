package com.example.summonerscompass.presentation.glossary_screen

import Champion
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.summonerscompass.network.DataDragonApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

class GlossaryScreenViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db =
        FirebaseDatabase.getInstance("https://summoners-compass-default-rtdb.europe-west1.firebasedatabase.app")

    private val uid = auth.uid

    private val _glossary = MutableStateFlow<List<Champion?>>(emptyList())
    val glossary: StateFlow<List<Champion?>> = _glossary

    private val _squares = MutableStateFlow<List<Bitmap>>(emptyList())
    val squares: StateFlow<List<Bitmap>> = _squares

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val originalGlossary = mutableListOf<Champion?>() // Backup da lista completa
    private val originalSquares = mutableListOf<Bitmap>()

    init {
        getGlossary()
    }

    fun getGlossary() {
        if (_glossary.value.isNotEmpty()) {
            _glossary.value = originalGlossary
            _squares.value = originalSquares
            return
        }

        _isLoading.value = true

        uid?.let { uid ->
            db.reference.child("users").child(uid).child("glossary")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            println("Glossary vazio ou não encontrado.")
                            _isLoading.value = false
                            return
                        }
                        val glossaryList =
                            snapshot.children.mapNotNull { it.getValue(String::class.java) }
                                .map { it.replace(" ", "").replace("'", "") }

                        getGlossaryChamps(glossaryList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Log de erro
                        println("Erro ao acessar o Firebase: ${error.message}")
                        _isLoading.value = false
                    }
                })
        } ?: run {
            println("Usuário não autenticado.")
        }
    }

    private fun getGlossaryChamps(glossary: List<String>) {
        viewModelScope.launch {
            try {
                val championList = ArrayList<Champion?>()
                val squareList = ArrayList<Bitmap>()

                for (name in glossary) {
                    try {
                        val championResponse = DataDragonApi.retrofitService.getChampion(name)
                        val champion = championResponse?.data?.get(name)

                        if (champion != null) {
                            championList.add(champion)

                            val res = champion.image.full
                            val responseBody: ResponseBody =
                                DataDragonApi.retrofitService.getChampionSquare(res)

                            val inputStream = responseBody.byteStream()
                            val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
                            squareList.add(bitmap)
                        } else {
                            println("Campeão não encontrado: $name")
                        }
                    } catch (e: Exception) {
                        println("Erro ao buscar dados do campeão $name: ${e.message}")
                    }
                }

                originalGlossary.clear()
                originalGlossary.addAll(championList)

                originalSquares.clear()
                originalSquares.addAll(squareList)

                _glossary.value = championList
                _squares.value = squareList

            } catch (e: Exception) {
                e.printStackTrace()
                println("Erro geral ao buscar campeões: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        if (query.isEmpty()) {
            _glossary.value = originalGlossary
            _squares.value = originalSquares
        } else {
            val filteredChampions = originalGlossary.filter { it?.name?.contains(query, ignoreCase = true) == true }
            val filteredSquares = filteredChampions.mapNotNull { champion ->
                val index = originalGlossary.indexOf(champion)
                if (index != -1) originalSquares.getOrNull(index) else null
            }

            _glossary.value = filteredChampions
            _squares.value = filteredSquares
        }
    }
}
