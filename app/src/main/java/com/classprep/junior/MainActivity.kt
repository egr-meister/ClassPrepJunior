package com.classprep.junior

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.classprep.junior.data.prefs.SetupProgress
import com.classprep.junior.data.reminders.ReminderNotifier
import com.classprep.junior.ui.AppNavHost
import com.classprep.junior.ui.theme.ClassPrepTheme
import com.classprep.junior.ui.theme.Paper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val pendingOpenDate = MutableStateFlow<LocalDate?>(null)

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            (application as ClassPrepApp).container.timeChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ClassPrepApp).container
        val setup = container.settings.setup.stateIn(lifecycleScope, SharingStarted.Eagerly, null)
        // The launch screen stays only until the saved setup state has been read (no artificial delay).
        splash.setKeepOnScreenCondition { setup.value == null }
        if (savedInstanceState == null) readOpenDate(intent)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = container.timeChanged()
        })

        setContent {
            ClassPrepTheme {
                val progress: SetupProgress? by setup.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize().background(Paper.Background)) {
                    progress?.let { AppNavHost(it, pendingOpenDate) }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(this, timeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        unregisterReceiver(timeReceiver)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readOpenDate(intent)
    }

    private fun readOpenDate(intent: Intent?) {
        val raw = intent?.getStringExtra(ReminderNotifier.EXTRA_TARGET_DATE) ?: return
        pendingOpenDate.value = runCatching { LocalDate.parse(raw) }.getOrNull()
    }
}
