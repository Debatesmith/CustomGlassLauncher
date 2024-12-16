package com.example.glasslauncher

import android.content.Intent
import android.content.pm.ResolveInfo
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity

class ParseResolvedAppList(private val activity: ComponentActivity) {

    private val packageManager: PackageManager = activity.packageManager

    fun getAppList(): ArrayList<AppBlock> {
        val resolvedApplist: List<ResolveInfo> = packageManager
            .queryIntentActivities(
                Intent(Intent.ACTION_MAIN, null)
                    .addCategory(Intent.CATEGORY_LAUNCHER),
                0
            )

        val appList = ArrayList<AppBlock>()

        for (ri in resolvedApplist) {
            if (ri.activityInfo.packageName != activity.packageName) {
                val app = AppBlock(
                    ri.loadLabel(packageManager).toString(),
                    ri.activityInfo.loadIcon(packageManager),
                    ri.activityInfo.packageName
                )
                appList.add(app)
            }
        }

        return appList
    }
}