package io.homeassistant.companion.android.assist

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.accompanist.themeadapter.material.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.BaseActivity
import io.homeassistant.companion.android.assist.ui.AssistSheetView

@AndroidEntryPoint
class AssistActivity : BaseActivity() {

    private val viewModel: AssistViewModel by viewModels()

    companion object {
        const val TAG = "AssistActivity"

        private const val EXTRA_SERVER = "server"
        private const val EXTRA_PIPELINE = "pipeline"
        private const val EXTRA_START_LISTENING = "start_listening"

        fun newInstance(context: Context, serverId: Int = -1, pipelineId: String? = null, startListening: Boolean = true): Intent {
            return Intent(context, AssistActivity::class.java).apply {
                putExtra(EXTRA_SERVER, serverId)
                putExtra(EXTRA_PIPELINE, pipelineId)
                putExtra(EXTRA_START_LISTENING, startListening)
            }
        }
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        { viewModel.onPermissionResult(it) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        } // else handled by manifest attribute
        val isLocked = getSystemService<KeyguardManager>()?.isKeyguardLocked ?: false
        if (isLocked) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        }

        if (savedInstanceState == null) {
            viewModel.onCreate()
            // TODO intent extras
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MdcTheme {
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = MaterialTheme.colors.isLight
                SideEffect {
                    systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = useDarkIcons)
                }

                AssistSheetView(
                    conversation = viewModel.conversation,
                    inputMode = viewModel.inputMode,
                    onSelectPipeline = viewModel::changePipeline,
                    onChangeInput = viewModel::onChangeInput,
                    onTextInput = viewModel::onTextInput,
                    onMicrophoneInput = viewModel::onMicrophoneInput,
                    onHide = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setPermissionInfo(
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) { requestPermission.launch(Manifest.permission.RECORD_AUDIO) }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
}
