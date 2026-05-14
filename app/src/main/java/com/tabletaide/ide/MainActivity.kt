package com.tabletaide.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.tabletaide.ide.ui.AppLaunchSurface
import com.tabletaide.ide.ui.IdeViewModel
import com.tabletaide.ide.ui.StartupGatewayScreen
import com.tabletaide.ide.ui.TabletIdeScreen
import com.tabletaide.ide.ui.theme.KineticTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ideVm: IdeViewModel = hiltViewModel()
            val themeMode by ideVm.themeMode.collectAsState()
            val appLaunchSurface by ideVm.appLaunchSurface.collectAsState()
            val recentWorkspaces by ideVm.recentWorkspaces.collectAsState()
            val status by ideVm.status.collectAsState()
            val cloneUiState by ideVm.cloneUiState.collectAsState()
            KineticTheme(mode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (appLaunchSurface) {
                        AppLaunchSurface.BOOTING -> Box(modifier = Modifier.fillMaxSize())
                        AppLaunchSurface.STARTUP_GATEWAY -> StartupGatewayScreen(
                            recentWorkspaces = recentWorkspaces,
                            statusMessage = status,
                            cloneUiState = cloneUiState,
                            checkAllFilesAccess = ideVm::hasAllFilesAccess,
                            onPeekSavedGitAuth = ideVm::peekSavedGitAuth,
                            onClearSavedGitAuth = ideVm::clearSavedGitAuth,
                            onClearCloneFeedback = ideVm::clearCloneFeedback,
                            onOpenWorkspace = ideVm::openWorkspaceRoot,
                            onOpenRecentWorkspace = ideVm::openRecentWorkspace,
                            onCreateStarterProject = ideVm::createStarterProject,
                            onCloneRepository = ideVm::cloneRepository,
                        )
                        AppLaunchSurface.IDE_SHELL -> TabletIdeScreen(ideVm = ideVm)
                    }
                }
            }
        }
    }
}
