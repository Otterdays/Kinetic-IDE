package com.tabletaide.ide

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tabletaide.ide.data.GitHubOAuthConfig
import com.tabletaide.ide.ui.theme.KineticTheme

/**
 * In-app GitHub OAuth — intercepts the custom-scheme redirect that Chrome Custom Tabs
 * often fails to hand back to the host app.
 */
class GitHubOAuthActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authUrl = intent.getStringExtra(EXTRA_AUTH_URL)?.trim().orEmpty()
        if (authUrl.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            KineticTheme {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean = handleRedirect(request?.url)

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    url: String?,
                                ): Boolean = handleRedirect(url?.let(Uri::parse))

                                private fun handleRedirect(uri: Uri?): Boolean {
                                    if (uri == null || !isOAuthCallback(uri)) return false
                                    setResult(Activity.RESULT_OK, Intent().setData(uri))
                                    finish()
                                    return true
                                }
                            }
                            loadUrl(authUrl)
                        }
                    },
                )
            }
        }
    }

    private fun isOAuthCallback(uri: Uri): Boolean =
        uri.scheme == GitHubOAuthConfig.REDIRECT_SCHEME &&
            uri.host == GitHubOAuthConfig.REDIRECT_HOST &&
            uri.path.orEmpty().startsWith(GitHubOAuthConfig.REDIRECT_PATH)

    companion object {
        const val EXTRA_AUTH_URL = "auth_url"

        fun createIntent(context: Context, authUrl: String): Intent =
            Intent(context, GitHubOAuthActivity::class.java)
                .putExtra(EXTRA_AUTH_URL, authUrl)
    }
}
