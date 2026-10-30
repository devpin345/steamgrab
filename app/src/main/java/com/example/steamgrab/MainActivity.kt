package com.example.steamgrab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SteamGrabApp()
                }
            }
        }
    }
}

@Composable
fun SteamGrabApp(vm: SteamViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()

    when (state) {
        is UiState.LoggedOut -> LoginScreen(
            loading = (state as UiState.LoggedOut).loading,
            error = (state as UiState.LoggedOut).error,
            needsGuardCode = (state as UiState.LoggedOut).needsGuardCode,
            onLogin = { user, pass, guard -> vm.login(user, pass, guard) }
        )
        is UiState.GameList -> GameListScreen(
            games = (state as UiState.GameList).games,
            onSelect = { appId -> vm.startDownload(appId) }
        )
        is UiState.Downloading -> DownloadScreen(
            progress = (state as UiState.Downloading).progress,
            gameName = (state as UiState.Downloading).gameName,
            status = (state as UiState.Downloading).status
        )
        is UiState.Done -> DoneScreen(
            path = (state as UiState.Done).savedPath,
            onBack = { vm.backToList() }
        )
    }
}

@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    needsGuardCode: Boolean,
    onLogin: (String, String, String?) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var guardCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login Steam", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Username") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (needsGuardCode) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = guardCode, onValueChange = { guardCode = it },
                label = { Text("Kode Steam Guard (email/app)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onLogin(username, password, guardCode.ifBlank { null }) },
            enabled = !loading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
            else Text("Login")
        }
    }
}

@Composable
fun GameListScreen(games: List<GameInfo>, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pilih Game", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(games) { game ->
                ListItem(
                    headlineContent = { Text(game.name) },
                    supportingContent = { Text("App ID: ${game.appId} • ${game.sizeLabel}") },
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
                Divider()
            }
        }
    }
}

@Composable
fun DownloadScreen(progress: Float, gameName: String, status: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(gameName, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("${(progress * 100).toInt()}% — $status")
    }
}

@Composable
fun DoneScreen(path: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Download selesai!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Tersimpan di:\n$path")
        Spacer(Modifier.height(16.dp))
        Text("Hubungkan HP ke PC via USB, lalu copy folder ini ke steamapps/common/<NamaGame> di PC, kemudian verify integrity lewat Steam client.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Download game lain") }
    }
}
