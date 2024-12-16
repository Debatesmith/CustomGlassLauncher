package com.example.glasslauncher

import android.content.Intent
import android.content.pm.ResolveInfo
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity

class ResolvedAppList(private val activity: ComponentActivity) {
    private val packageManager: PackageManager = activity.packageManager

    fun getResolvedAppList(): List<ResolveInfo> {
        return packageManager
            .queryIntentActivities(
                Intent(Intent.ACTION_MAIN, null)
                    .addCategory(Intent.CATEGORY_LAUNCHER),
                0
            )
    }
}