package com.dimonoso.crosplatformfilesender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import android.content.Intent
import com.dimonoso.crosplatformfilesender.backup.bindAndroidBackupContext
import com.dimonoso.crosplatformfilesender.platform.bindAndroidFolderPickerActivity
import com.dimonoso.crosplatformfilesender.platform.bindAndroidPlatformContext
import com.dimonoso.crosplatformfilesender.platform.handleAndroidFolderPickerResult
import com.dimonoso.crosplatformfilesender.settings.bindAndroidSettingsContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        bindAndroidSettingsContext(applicationContext)
        bindAndroidBackupContext(applicationContext)
        bindAndroidPlatformContext(applicationContext)
        bindAndroidFolderPickerActivity(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (handleAndroidFolderPickerResult(requestCode, resultCode, data)) return

        super.onActivityResult(requestCode, resultCode, data)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
