package com.example.monero_jetpack // Change the package name here

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.monero_jetpack.ui.theme.Monero_jetpackTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Monero_jetpackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {

val context = LocalContext.current

Box(modifier = Modifier.fillMaxSize()){

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    }){
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(text = "Settings", fontSize = 25.sp)
                },

                colors = TopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                    scrolledContainerColor= MaterialTheme.colorScheme.secondaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,

                    )
            )
        },

        content = { innerPadding ->
            SettingsBoard(paddingValues = innerPadding)



        },
    )

}
}



@Composable
fun SettingsBoard(paddingValues: PaddingValues) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.secondary,
                disabledContentColor = MaterialTheme.colorScheme.secondary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = true, onClick = {})
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Data",
                    fontSize = 20.sp
                )

            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Appearance",
                    fontSize = 20.sp
                )

            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contact",
                    fontSize = 20.sp
                )

            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Info",
                    fontSize = 20.sp
                )

            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "About",
                    fontSize = 20.sp
                )

            }
            HorizontalDivider()

        }




    }




}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen() }