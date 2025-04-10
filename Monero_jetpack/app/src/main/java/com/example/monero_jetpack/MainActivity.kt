package com.example.monero_jetpack
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monero_jetpack.ui.theme.Monero_jetpackTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.zIndex
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import android.provider.Settings  // For Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.ZeroLineProperties


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Monero_jetpackTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}


data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel:WalletViewModel = viewModel()) {
    val accountName by viewModel.accountName.collectAsState()
    val accountBalance by viewModel.accountBalance.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    val context = LocalContext.current
    var selectedItem by remember { mutableIntStateOf(0) }
    val items1 = listOf("Home", "Contacts", "Payments")
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Person, Icons.Filled.Info)
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.Person, Icons.Outlined.Info)
    var showQRCode by remember { mutableStateOf(false) } // <-- Lifted State
    var showPayment by remember { mutableStateOf(false) } // <-- Lifted State

    val navigationBarHeight = remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current


    var flag by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {


        Scaffold(
            Modifier.zIndex(0f),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Monero Wallet", fontSize = 25.sp)
                    },
                    actions = {
                        IconButton(onClick = {
                            val intent = Intent(context, UserLoginActivity::class.java)
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Login"
                            )
                        }

                        IconButton(onClick = {
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    colors = TopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
            },
            bottomBar = {
                NavigationBar (modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                    navigationBarHeight.value = with(density) { layoutCoordinates.size.height.toDp() }}
                ){

                    items1.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                    contentDescription = item
                                )
                            },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index},
                        )

                    }
                }
            },
            content = { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    Dashboard(paddingValues = innerPadding,
                        flag = flag,
                        onToggleFlag = { flag = !flag },
                        accountName = accountName,
                        accountBalance = accountBalance,
                        isLoading = isLoading,
                        error = error,
                        transactions = transactions)


                }
            }
        )

        // Floating Action Button (FAB) - Placed Last to Ensure Visibility
        FloatingActionButton(
            onClick = { flag = !flag },
            containerColor = MaterialTheme.colorScheme.inversePrimary,
            contentColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
            modifier = Modifier
                .align(Alignment.BottomEnd) // FAB Position
                .padding(end=20.dp, bottom = navigationBarHeight.value +20.dp)
                .zIndex(2f) // Ensure it's above everything, including the dim overlay
        ) {
            Icon(
                imageVector = if (flag) Icons.Filled.Clear else Icons.Filled.Add,
                contentDescription = "Menu Toggle"
            )
        }

        // Dimming overlay (covers EVERYTHING, including the AppBar and BottomBar)
        if(flag ||showQRCode||showPayment){
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)) // Dim effect
                    .clickable { flag = false } // Click outside to dismiss
                    .zIndex(1f) // Below FAB, above Scaffold
            )
        }
        if (flag ) {

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 30.dp, end = 28.dp)
                    .zIndex(2f) // Above the dim effect
            ) {
                FloatingMenu(onDismiss = {flag = false},
                    onShowQRCode = { showQRCode = true  },
                    onShowPayment = {showPayment = true},
                    navBarHeight = navigationBarHeight.value // Pass the height here
                )
            }

        }




    }
    // Show QR Code Fullscreen When Activated
    if (showQRCode) {
        QRCodeScreen("https://yourwebsite.com", onDismiss = { showQRCode = false })
    }
    if(showPayment){
        TransactionCard(onClose = { showPayment = false }, onSend = { qrCode, amount -> showPayment = false })
    }
}



@Composable
fun Dashboard(paddingValues: PaddingValues, flag: Boolean,
              onToggleFlag: () -> Unit,
              accountName: String,
              accountBalance: String,
              isLoading: Boolean,
              error: String?,
              transactions: List<Transaction>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp, start = 16.dp, end = 16.dp, bottom = 16.dp), // Remove top padding
        verticalArrangement = Arrangement.Top,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.onSurface,

            ),
            border = BorderStroke(1.dp, Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = true, onClick = {})
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Account Name",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White

                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = accountBalance,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            // Add the chart after data has loaded
            TransactionDeltaLineChart( viewModel= WalletViewModel())
        }
    }
}

fun List<Transaction>.toChartLine(): Line {
    val values = map { tx ->
        if (tx.type == "in") tx.amount else -tx.amount
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color.Green, Color.Red),
        startY = 0f,
        tileMode = TileMode.Clamp
    )

    return Line(
        label = "Balance Change",
        values = values + (-0.5)+ 0.5 + (-0.5)+0.5,
        color = gradientBrush,
        firstGradientFillColor = Color.Green.copy(alpha = 0.3f),
        secondGradientFillColor = Color.Red.copy(alpha = 0.3f),
        strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
        gradientAnimationDelay = 1000,
    )
}


@Composable
fun TransactionDeltaLineChart(viewModel: WalletViewModel) {
    val transactions by viewModel.transactions.collectAsState()

    if (transactions.isNotEmpty()) {
        val line = remember(transactions) { listOf(transactions.toChartLine()) }

        LineChart(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            data = line,
            animationMode = AnimationMode.Together(delayBuilder = { it * 500L }),
            zeroLineProperties = ZeroLineProperties(
                enabled = true,
                color = SolidColor(Color.Red),
            ),
            minValue = -1.0,
            maxValue = 1.0

        )

    }
}


@Composable
fun FloatingMenu(onDismiss: () -> Unit, onShowQRCode: () -> Unit,onShowPayment: ()->Unit,navBarHeight: Dp){
    // Floating Menu
    var showQRCode by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .background(color = Color.Transparent)
            .padding(bottom = navBarHeight+50.dp).zIndex(4f),
        horizontalAlignment = Alignment.End
    ) {

        Row(modifier= Modifier.padding(bottom = 30.dp)) {
            Text(
                "Send", modifier = Modifier
                    .clickable { onShowPayment(); onDismiss() }
                    .align(Alignment.CenterVertically)
                    .padding(end = 20.dp),
                color = Color.White
            )
            FloatingActionButton(
                onClick = {onShowPayment(); onDismiss()  },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                modifier = Modifier.size(40.dp)

            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Localized description"
                )
            }
        }
        Row(modifier= Modifier.padding(bottom = 30.dp)) {
            Text(
                "Receive", modifier = Modifier
                    .clickable { onShowQRCode(); onDismiss() }
                    .align(Alignment.CenterVertically)
                    .padding(end = 20.dp),
                color = Color.White
            )
            FloatingActionButton(
                onClick = { onShowQRCode(); onDismiss() },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp)

            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Localized description"
                )
            }

        }
    }
}


fun generateQRCode(text: String, size: Int = 512): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun QRCodeScreen(inputText: String, onDismiss: () -> Unit) {
    val qrBitmap = remember(inputText) { generateQRCode(inputText) }
    var isVisible by remember { mutableStateOf(false) }

    // Delay animation until composable is fully loaded
    LaunchedEffect(Unit) {
        isVisible = true
    }
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it }, // Start from bottom of screen
                animationSpec = tween(durationMillis = 500) // Smooth animation
            ),
            exit = slideOutVertically(
                targetOffsetY = { it }, // Exit to bottom
                animationSpec = tween(durationMillis = 300)
            )
        ) {
    Box( // Ensure full screen coverage
        modifier = Modifier
            .fillMaxSize()
            .zIndex(3f)
            .clickable {
                isVisible = false // Trigger exit animation
                onDismiss()
            }

    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(0.dp) // Ensure no extra padding
                .align(alignment = Alignment.BottomCenter)
                .clickable(enabled = false) {} // Prevents clicks on the card from closing it

        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                qrBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Generated QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = inputText, color = Color.White)
            }
        }
    }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TransactionCard(
    onClose: () -> Unit,
    onSend: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var qrCodeText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    // Camera permission state - using the new API
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Result launcher for QR scanner
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            qrCodeText = result.data?.getStringExtra("SCAN_RESULT") ?: ""
        }
    }

    // Check permission when launched
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Close Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // QR Code Input with one button for scanning
                OutlinedTextField(
                    value = qrCodeText,
                    onValueChange = { qrCodeText = it },
                    label = { Text("QR Code") },
                    trailingIcon = {
                        // Only one button to scan QR code, checking for permission before launching
                        when {
                            permissionState.status.isGranted -> {
                                IconButton(onClick = {
                                    val intent = Intent(context, QrScannerActivity::class.java)
                                    scanLauncher.launch(intent)
                                }) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.qr_code_24px),
                                        contentDescription = "Scan QR Code"
                                    )
                                }
                            }
                            permissionState.status.shouldShowRationale -> {
                                // If permission is denied and should show rationale, display rationale button
                                IconButton(onClick = { permissionState.launchPermissionRequest() }) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Grant Camera Permission"
                                    )
                                }
                            }
                            else -> {
                                // If permission is denied and we can't show rationale, show button to open settings
                                IconButton(onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Open Settings"
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { onSend(qrCodeText, amountText) }
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}




@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen() }