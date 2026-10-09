package com.astral.wbtn

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.astral.wbtn.api.WebtoonRepository
import com.astral.wbtn.download.Downloader
import com.astral.wbtn.ui.MainScreen

class MainActivity : ComponentActivity() {

    private val repository by lazy { WebtoonRepository() }
    private val downloader by lazy { Downloader(applicationContext, repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkStoragePermission()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00DC64),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B)
                )
            ) {
                Surface {
                    MainScreen(repository = repository, downloader = downloader)
                }
            }
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
            }
        }
    }
}
