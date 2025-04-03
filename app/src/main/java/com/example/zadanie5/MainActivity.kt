package com.example.zadanie5

import android.os.Bundle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import coil.compose.AsyncImage
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import retrofit2.http.Path
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL = "https://rickandmortyapi.com/api/"

// Модели данных
data class Character(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val image: String
)

data class CharacterResponse(val results: List<Character>)

// Интерфейс API
interface RickMortyApiService {
    @GET("character")
    suspend fun getCharacters(@Query("page") page: Int): CharacterResponse

    @GET("character/{id}")
    suspend fun getCharacterById(@Path("id") id: Int): Character
}

// Инициализация Retrofit
object RetrofitInstance {
    val api: RickMortyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RickMortyApiService::class.java)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Инициализация NavController
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "characters") {
                composable("characters") {
                    // Здесь UI для списка персонажей
                    CharacterList(navController)
                }
                composable("character_detail/{characterId}") { backStackEntry ->
                    val characterId = backStackEntry.arguments?.getString("characterId")?.toInt()
                    // Показать подробности персонажа
                    CharacterDetail(characterId)
                }
            }
        }
    }
}

// Экран списка персонажей
@Composable
fun CharacterList(navController: NavController) {
    var characters by remember { mutableStateOf<List<Character>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitInstance.api.getCharacters(1)
            characters = response.results
        } catch (e: Exception) {
            errorMessage = "Ошибка загрузки: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (!errorMessage.isNullOrEmpty()) {
            Text(text = errorMessage!!, color = Color.Red)
        } else {
            LazyColumn {
                items(characters) { character ->
                    // Передаем navController в CharacterCard
                    CharacterCard(character = character, navController = navController)
                }
            }
        }
    }
}

// Карточка персонажа с переходом на детали
@Composable
fun CharacterCard(character: Character, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                navController.navigate("character_detail/${character.id}")
            },
        elevation = 4.dp
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Image(
                painter = rememberAsyncImagePainter(character.image),
                contentDescription = character.name,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = character.name, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Статус: ${character.status}")
                Text(text = "Раса: ${character.species}")
            }
        }
    }
}

// Экран деталей персонажа
@Composable
fun CharacterDetail(characterId: Int?) {
    var character by remember { mutableStateOf<Character?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(characterId) {
        try {
            if (characterId != null) {
                character = RetrofitInstance.api.getCharacterById(characterId)
            }
        } catch (e: Exception) {
            errorMessage = "Ошибка загрузки: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Используем Box для выравнивания содержимого
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (errorMessage != null) {
            Text(text = errorMessage!!, color = Color.Red, modifier = Modifier.align(Alignment.Center))
        } else {
            character?.let {
                // Отображение информации о персонаже
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Text(text = "Имя: ${it.name}")
                    Text(text = "Статус: ${it.status}")
                    Text(text = "Раса: ${it.species}")
                    AsyncImage(
                        model = it.image,
                        contentDescription = it.name,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
            }
        }
    }
}
