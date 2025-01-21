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
    private val db = FirebaseDatabase.getInstance("https://summoners-compass-default-rtdb.europe-west1.firebasedatabase.app")

    private val uid = auth.uid

    private val _glossary = MutableStateFlow<List<Champion?>>(emptyList())
    val glossary: StateFlow<List<Champion?>> = _glossary

    private val _squares = MutableStateFlow<List<Bitmap>>(emptyList())
    val squares: StateFlow<List<Bitmap>> = _squares

    init {
        getGlossary()
    }

    fun getGlossary() {
        uid?.let { uid ->
            db.reference.child("users").child(uid).child("glossary")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Verifique se o snapshot contém dados
                        if (!snapshot.exists()) {
                            println("Glossary vazio ou não encontrado.")
                            return
                        }

                        // Obtenha a lista de campeões do glossário e sanitize os nomes
                        val glossaryList = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                            .map { it.replace(" ", "").replace("'", "") }

                        // Chame o método para buscar detalhes dos campeões
                        getGlossaryChamps(glossaryList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Log de erro
                        println("Erro ao acessar o Firebase: ${error.message}")
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

                _squares.value = squareList
                _glossary.value = championList
            } catch (e: Exception) {
                e.printStackTrace()
                println("Erro geral ao buscar campeões: ${e.message}")
            }
        }
    }
}
