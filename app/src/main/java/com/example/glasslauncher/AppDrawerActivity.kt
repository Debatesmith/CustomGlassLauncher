package com.example.glasslauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.glasslauncher.ui.theme.GlassLauncherTheme
import android.view.MotionEvent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import android.view.View
import android.view.WindowManager
import android.content.Intent

class AppDrawerActivity : ComponentActivity(), GlassGestureDetector.OnGestureListener {

    private lateinit var glassGestureDetector: GlassGestureDetector
    private val selectedIndexState = mutableStateOf(0)
    private lateinit var appList: List<AppBlock>
    private var emptySlotIndex = -1

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

        emptySlotIndex = intent.getIntExtra("empty_slot_index", -1)

        setContent {
            appList = remember { ParseResolvedAppList(this).getAppList() }
            val currentIndex = selectedIndexState.value

            GlassLauncherTheme {
                AppDrawerScreen(this, appList, currentIndex, onAppSelected = { launchApp(it) })
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
                selectedIndexState.value = (selectedIndexState.value + 1) % appList.size
                true
            }
            GlassGestureDetector.Gesture.SWIPE_BACKWARD -> {
                selectedIndexState.value = (selectedIndexState.value - 1 + appList.size) % appList.size
                true
            }
            GlassGestureDetector.Gesture.TAP -> {
                launchApp(selectedIndexState.value)
                true
            }
            GlassGestureDetector.Gesture.SWIPE_DOWN -> {
                finish()
                true
            }
            else -> false
        }
    }

    private fun launchApp(index: Int) {
        if (index >= 0 && index < appList.size) {
            val selectedApp = appList[index]
            val resultIntent = Intent()
            resultIntent.putExtra("selected_app", selectedApp)
            resultIntent.putExtra("empty_slot_index", emptySlotIndex)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}


@Composable
fun AppDrawerScreen(activity: ComponentActivity, apps: List<AppBlock>, selectedIndex: Int, onAppSelected: (Int) -> Unit) {

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            AppList(apps = apps, selectedIndex = selectedIndex, onAppSelected = onAppSelected)
        }
    }
}

@Composable
fun AppList(apps: List<AppBlock>, selectedIndex: Int, onAppSelected: (Int) -> Unit) {
    LazyColumn {
        itemsIndexed(apps) { index, app ->
            AppListItem(app = app, isSelected = index == selectedIndex, onAppSelected = onAppSelected, index = index)
        }
    }
}

@Composable
fun AppListItem(app: AppBlock, isSelected: Boolean, onAppSelected: (Int) -> Unit, index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                color = if (isSelected) Color.DarkGray.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageBitmap = remember(app.icon) {
            app.icon.toBitmap().asImageBitmap()
        }

        Image(
            bitmap = imageBitmap,
            contentDescription = app.appName,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = app.appName,
            fontSize = 16.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
    }
}