package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.InstagramAppTheme

private const val INSTAGRAM_HOME_URL = "https://www.instagram.com/"
private const val INSTAGRAM_EMAIL_URL =
    "https://accountscenter.instagram.com/youraccount/contact_points/?entrypoint=profile_page&is_from_dialog=true"
private const val INSTAGRAM_2FA_URL =
    "https://accountscenter.instagram.com/password_and_security/two_factor/"

class MainActivity : ComponentActivity() {

    private var webViewInstance: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            InstagramAppTheme {
                InstagramScreen(
                    onRegisterWebView = { webViewInstance = it },
                    getWebView = { webViewInstance }
                )
            }
        }
    }

    override fun onDestroy() {
        webViewInstance?.let { wv ->
            wv.stopLoading()
            wv.clearHistory()
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.destroy()
        }
        webViewInstance = null
        super.onDestroy()
    }
}

@Composable
fun InstagramScreen(
    onRegisterWebView: (WebView) -> Unit,
    getWebView: () -> WebView?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var canGoBack by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }

    // Hardware/System back button navigates web history if possible
    BackHandler(enabled = canGoBack) {
        val webView = getWebView()
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Flat Linear Loading Bar (no glow, minimal 2dp)
            AnimatedVisibility(
                visible = isLoading && pageProgress < 1f,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { pageProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .testTag("page_loading_indicator"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }

            // Smooth, responsive mobile WebView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                InstagramWebView(
                    initialUrl = INSTAGRAM_HOME_URL,
                    onProgressChanged = { progress ->
                        pageProgress = progress / 100f
                        isLoading = progress < 100
                    },
                    onCanGoBackChanged = { canBack ->
                        canGoBack = canBack
                    },
                    onWebViewCreated = onRegisterWebView,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Flat Bottom Control Panel
            BottomControlPanel(
                onEmailClick = {
                    getWebView()?.loadUrl(INSTAGRAM_EMAIL_URL)
                },
                onTwoFactorClick = {
                    getWebView()?.loadUrl(INSTAGRAM_2FA_URL)
                },
                onClearDataClick = {
                    clearAllWebViewData(context, getWebView()) {
                        getWebView()?.loadUrl(INSTAGRAM_HOME_URL)
                        Toast.makeText(
                            context,
                            context.getString(R.string.data_cleared),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramWebView(
    initialUrl: String,
    onProgressChanged: (Int) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("instagram_webview"),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // High-performance, lightweight WebView settings
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = false
                    allowContentAccess = false
                }

                // Smooth cookie management
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        // Allow http and https inside WebView
                        return if (url.startsWith("http://") || url.startsWith("https://")) {
                            false
                        } else {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                ctx.startActivity(intent)
                            } catch (_: Exception) {
                                // Fallback ignore unsupported URI schemes safely
                            }
                            true
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onCanGoBackChanged(view?.canGoBack() == true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onCanGoBackChanged(view?.canGoBack() == true)
                    }
                }

                loadUrl(initialUrl)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            onCanGoBackChanged(webView.canGoBack())
        }
    )
}

@Composable
fun BottomControlPanel(
    onEmailClick: () -> Unit,
    onTwoFactorClick: () -> Unit,
    onClearDataClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Clean, flat control panel with no glow, minimal border, and flat background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("bottom_control_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlatControlButton(
                text = stringResource(R.string.btn_email),
                icon = Icons.Default.Email,
                onClick = onEmailClick,
                testTag = "email_button",
                modifier = Modifier.weight(1f)
            )

            FlatControlButton(
                text = stringResource(R.string.btn_2fa),
                icon = Icons.Default.Lock,
                onClick = onTwoFactorClick,
                testTag = "two_factor_button",
                modifier = Modifier.weight(1f)
            )

            FlatControlButton(
                text = stringResource(R.string.btn_clear_data),
                icon = Icons.Default.CleaningServices,
                onClick = onClearDataClick,
                testTag = "clear_data_button",
                isDestructive = true,
                modifier = Modifier.weight(1.3f)
            )
        }
    }
}

@Composable
fun FlatControlButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val backgroundColor = if (isDestructive) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isDestructive) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .height(48.dp) // Meets strict 48dp accessibility touch target
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            )
            .testTag(testTag)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Clears stored cookies, session cache, local web storage, and browsing history.
 */
fun clearAllWebViewData(context: Context, webView: WebView?, onDone: () -> Unit) {
    try {
        // Clear all cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies {
            cookieManager.flush()
        }

        // Clear WebStorage (localStorage, sessionStorage)
        WebStorage.getInstance().deleteAllData()

        // Clear WebView internal storage & cache
        webView?.apply {
            clearCache(true)
            clearFormData()
            clearHistory()
            clearSslPreferences()
        }

        // Clear application cache folder
        context.cacheDir.deleteRecursively()
    } catch (_: Exception) {
        // Safe catch to ensure app never crashes on clearing
    } finally {
        onDone()
    }
}
