package com.example.glasslauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.glasslauncher.ui.theme.GlassLauncherTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.view.MotionEvent
import android.util.Log
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.input.pointer.pointerInput
import android.view.View
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

class MainActivity : ComponentActivity(), GlassGestureDetector.OnGestureListener {
    private lateinit var glassGestureDetector: GlassGestureDetector
    private val selectedIndexState = mutableStateOf(0)
    private var maxApps = 12
    private val _appList = mutableStateListOf<AppBlock>()
    private val appList: List<AppBlock> get() = _appList
    private val _appPositions = mutableStateMapOf<Int, AppBlock?>()
    private val appPositions: Map<Int, AppBlock?> get() = _appPositions
    private lateinit var sharedPreferences: SharedPreferences

    private val appDrawerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val selectedApp = data?.getParcelableExtra<AppBlock>("selected_app")
            val emptySlotIndex = data?.getIntExtra("empty_slot_index", -1) ?: -1

            if (selectedApp != null && emptySlotIndex != -1) {
                _appPositions[emptySlotIndex] = selectedApp
                saveAppPositions()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request touchpad access and set up window flags
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Set immersive sticky mode
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                )

        // Initialize gesture detector
        glassGestureDetector = GlassGestureDetector(this, this)
        sharedPreferences = getSharedPreferences("app_positions", Context.MODE_PRIVATE)

        // Load saved app positions
        val initialAppList = ParseResolvedAppList(this).getAppList()
        val savedAppPositions = mutableMapOf<Int, AppBlock?>()
        for (i in 0 until maxApps) {
            val packageName = sharedPreferences.getString("app_$i", null)
            savedAppPositions[i] = if (packageName != null) {
                initialAppList.find { it.packageName == packageName }
            } else {
                null
            }
        }
        _appPositions.putAll(savedAppPositions)
        _appList.addAll(initialAppList)

        setContent {
            val currentIndex = selectedIndexState.value

            GlassLauncherTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    HomeScreen(
                        apps = appPositions,
                        selectedIndex = currentIndex,
                        onAppSelected = { launchApp(it) },
                        onAppRemoved = { removeApp(it) }
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return glassGestureDetector.onTouchEvent(ev) || super.dispatchTouchEvent(ev)
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean {
        return when (gesture) {
            GlassGestureDetector.Gesture.SWIPE_FORWARD -> {
                selectedIndexState.value = (selectedIndexState.value + 1) % maxApps
                true
            }
            GlassGestureDetector.Gesture.SWIPE_BACKWARD -> {
                selectedIndexState.value = (selectedIndexState.value - 1 + maxApps) % maxApps
                true
            }
            GlassGestureDetector.Gesture.SWIPE_DOWN -> {
                finish()
                true
            }
            GlassGestureDetector.Gesture.TAP -> {
                launchApp(selectedIndexState.value)
                true
            }
            GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_UP -> {
                openAppDrawer()
                true
            }
            GlassGestureDetector.Gesture.TAP_AND_HOLD -> {
                handleTapAndHold()
                true
            }
            GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_DOWN -> {
                launchGoogleAssistant()
                true
            }
            else -> false
        }
    }

    private fun handleTapAndHold() {
        val selectedIndex = selectedIndexState.value
        if (appPositions[selectedIndex] == null) {
            openAppDrawer(selectedIndex)
        } else {
            removeApp(selectedIndex)
        }
    }

    private fun launchApp(index: Int) {
        if (index >= 0 && index < appPositions.size) {
            val selectedApp = appPositions[index]
            if (selectedApp != null) {
                val launchIntent = packageManager.getLaunchIntentForPackage(selectedApp.packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    Log.e("GlassLauncher", "Could not create launch intent for ${selectedApp.packageName}")
                }
            }
        }
    }
    private fun removeApp(index: Int) {
        if (index >= 0 && index < appPositions.size) {
            _appPositions[index] = null
            saveAppPositions()
        }
    }

    private fun openAppDrawer() {
        val intent = Intent(this, AppDrawerActivity::class.java)
        startActivity(intent)
    }

    private fun openAppDrawer(emptySlotIndex: Int) {
        val intent = Intent(this, AppDrawerActivity::class.java)
        intent.putExtra("empty_slot_index", emptySlotIndex)
        appDrawerLauncher.launch(intent)
    }

    private fun saveAppPositions() {
        val editor = sharedPreferences.edit()
        for ((index, app) in appPositions) {
            editor.putString("app_$index", app?.packageName)
        }
        editor.apply()
    }

    private fun launchGoogleAssistant() {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("GlassLauncher", "Error launching Google Assistant", e)
            // Fallback to voice search if direct launch fails
            launchVoiceSearchFallback()
        }
    }


    private fun launchVoiceSearchFallback() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("android-app://com.google.android.googlequicksearchbox/https/www.google.com/search?q=search"))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("GlassLauncher", "Error launching voice search fallback", e)
        }
    }
}


@Composable
fun HomeScreen(
    apps: Map<Int, AppBlock?>,
    selectedIndex: Int,
    onAppSelected: (Int) -> Unit,
    onAppRemoved: (Int) -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize().background(Color.Black)) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCurrentTime(),
                    fontSize = 32.sp,
                    color = Color.White
                )

                RadialApps(
                    apps = apps,
                    selectedIndex = selectedIndex,
                    onAppSelected = onAppSelected,
                    onAppRemoved = onAppRemoved
                )
            }

            Text(
                text = getCurrentDate(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun RadialApps(
    apps: Map<Int, AppBlock?>,
    selectedIndex: Int,
    onAppSelected: (Int) -> Unit,
    onAppRemoved: (Int) -> Unit
) {
    Log.d("GlassLauncher", "RadialApps recomposing with selectedIndex: $selectedIndex")

    val radius = 150.dp
    val startAngle = 270f
    val totalPositions = 12 // Fixed total number of positions
    val angleStep = 360f / totalPositions // Angle step is fixed

    for (index in 0 until totalPositions) {
        val app = apps[index]
        val angle = (startAngle + (index * angleStep)) * (Math.PI / 180f)
        val x = (radius.value * cos(angle)).dp
        val y = (radius.value * sin(angle)).dp

        Box(
            modifier = Modifier
                .offset { IntOffset(x.roundToPx(), y.roundToPx()) }
                .size(48.dp)
        ) {
            if (index == selectedIndex) {
                Log.d("GlassLauncher", "Drawing selection indicator for index: $index")
                Box(
                    modifier = Modifier
                        .size(64.dp) // Increased size of the white circle
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        )
                )
            }

            if (app != null) {
                RadialAppItem(app)
            }
        }
    }
}


@Composable
fun RadialAppItem(app: AppBlock) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        val imageBitmap = remember(app.icon) {
            app.icon.toBitmap().asImageBitmap()
        }

        Image(
            bitmap = imageBitmap,
            contentDescription = app.appName,
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = app.appName,
            fontSize = 12.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// Helper functions
fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    return sdf.format(Date())
}

fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date())
}

// Single Preview
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val sampleApps = mapOf<Int, AppBlock?>()
    GlassLauncherTheme {
        HomeScreen(
            apps = sampleApps,
            selectedIndex = 0,
            onAppSelected = {},
            onAppRemoved = {}
        )
    }
}