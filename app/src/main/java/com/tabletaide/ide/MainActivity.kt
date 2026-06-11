package com.tabletaide.ide

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.tabletaide.ide.data.GitHubOAuthConfig
import com.tabletaide.ide.ui.AppLaunchSurface
import com.tabletaide.ide.ui.IdeViewModel
import com.tabletaide.ide.ui.StartupGatewayScreen
import com.tabletaide.ide.ui.TabletIdeScreen
import com.tabletaide.ide.ui.theme.KineticTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var oauthCallbackHandler: ((Uri) -> Unit)? = null

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
            val githubOAuthState by ideVm.githubOAuthState.collectAsState()

            DisposableEffect(Unit) {
                oauthCallbackHandler = { uri -> ideVm.completeGitHubSignIn(uri) }
                dispatchOAuthIntent(intent)
                onDispose { oauthCallbackHandler = null }
            }

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
                            githubOAuthState = githubOAuthState,
                            checkAllFilesAccess = ideVm::hasAllFilesAccess,
                            onPeekSavedGitAuth = ideVm::peekSavedGitAuth,
                            onClearSavedGitAuth = ideVm::clearSavedGitAuth,
                            onClearCloneFeedback = ideVm::clearCloneFeedback,
                            onOpenWorkspace = ideVm::openWorkspaceRoot,
                            onOpenRecentWorkspace = ideVm::openRecentWorkspace,
                            onCreateStarterProject = ideVm::createStarterProject,
                            onCloneRepository = ideVm::cloneRepository,
                            onBeginGitHubSignIn = ideVm::beginGitHubSignIn,
                            onGitHubSignOut = ideVm::signOutGitHub,
                            onLoadGitHubRepos = ideVm::loadGitHubRepos,
                            onCloneGitHubRepository = ideVm::cloneGitHubRepository,
                        )
                        AppLaunchSurface.IDE_SHELL -> TabletIdeScreen(ideVm = ideVm)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchOAuthIntent(intent)
    }

    private fun dispatchOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != GitHubOAuthConfig.REDIRECT_SCHEME ||
            uri.host != GitHubOAuthConfig.REDIRECT_HOST
        ) {
            return
        }
        oauthCallbackHandler?.invoke(uri)
        intent.data = null
    }
}
