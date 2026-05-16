package dev.brahmkshatriya.echo.ui.extensions

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.LinearProgressIndicator
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.WebViewRequest
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.NetworkRequest
import dev.brahmkshatriya.echo.databinding.FragmentGenericCollapsableBinding
import dev.brahmkshatriya.echo.databinding.FragmentWebviewBinding
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.addIfNull
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.extensions.login.LoginFragment.Companion.bind
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object WebViewUtils {
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    fun <T> FragmentActivity.configure(
        webView: WebView,
        progress: LinearProgressIndicator,
        target: WebViewRequest<T>,
        skipTimeout: Boolean,
        onComplete: suspend (Result<T>?) -> Unit,
    ): OnBackPressedCallback {
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                webView.goBack()
            }
        }
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().run {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            flush()
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            userAgentString = USER_AGENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                isAlgorithmicDarkeningAllowed = true
        }
        runCatching {
            webView.load(lifecycleScope, progress, callback, target, skipTimeout, onComplete)
        }.getOrElse {
            lifecycleScope.launch {
                webView.stop(callback)
                onComplete(Result.failure(it))
            }
        }
        return callback
    }

    private fun <T> WebView.load(
        scope: CoroutineScope,
        progress: LinearProgressIndicator,
        callback: OnBackPressedCallback,
        target: WebViewRequest<T>,
        skipTimeout: Boolean,
        onComplete: suspend (Result<T>?) -> Unit,
    ) {
        val stopRegex = target.stopUrlRegex
        val interceptRegex =
            if (target is WebViewRequest.Headers) target.interceptUrlRegex else null
        val timeout = target.maxTimeout
        val bridge = Bridge()
        val requests = mutableListOf<NetworkRequest>()
        val timeoutJob = if (!skipTimeout) scope.launch {
            delay(timeout)
            onComplete(
                Result.failure(
                    Exception(
                        "WebView request timed out after $timeout ms\nParsed Links:\n" +
                                requests.joinToString("\n") { it.url }
                    )
                )
            )
        } else null

        val mutex = Mutex()
        var done = false

        fun checkStop(url: String) {
            if (stopRegex.find(url) == null) return
            timeoutJob?.cancel()
            scope.launch(Dispatchers.IO) {
                mutex.withLock {
                    if (done) return@withLock
                    val result = runCatching {
                        when (target) {
                            is WebViewRequest.Cookie -> {
                                var cookie = ""
                                repeat(10) {
                                    cookie = CookieManager.getInstance().getCookie(url) ?: ""
                                    if (cookie.contains("SAPISID") || cookie.contains("__Secure-3PAPISID")) {
                                        return@repeat
                                    }
                                    delay(500)
                                }
                                target.onStop(
                                    NetworkRequest(url, method = NetworkRequest.Method.GET),
                                    cookie
                                )
                            }
                            is WebViewRequest.Evaluate -> {
                                target.onStop(
                                    NetworkRequest(url, method = NetworkRequest.Method.GET),
                                    evalJS(bridge, target.javascriptToEvaluate).getOrNull()
                                )
                            }
                            is WebViewRequest.Headers -> {
                                target.onStop(requests)
                            }
                            else -> null
                        }
                    }
                    val resultValue = result.getOrNull()
                    if (resultValue == null && result.isSuccess) return@withLock
                    done = true
                    onComplete(null)
                    stop(callback)
                    onComplete(result.map { it!! })
                }
            }
        }

        webViewClient = object : WebViewClient() {
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                callback.isEnabled = view?.canGoBack() ?: false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progress.show()
                if (done) return
                if (target is WebViewRequest.Evaluate) {
                    target.javascriptToEvaluateOnPageStart?.let { js ->
                        scope.launch {
                            runCatching { evalJS(null, js) }.onFailure {
                                stop(callback)
                                onComplete(Result.failure(it))
                            }
                        }
                    }
                }
                checkStop(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress.hide()
                if (!done && url != null) checkStop(url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?,
            ): Boolean {
                request?.url?.toString()?.let { checkStop(it) }
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView, request: WebResourceRequest,
            ): WebResourceResponse? {
                if (target is WebViewRequest.Headers) {
                    val url = request.url.toString()
                    if (interceptRegex == null || interceptRegex.matches(url)) {
                        requests.add(
                            NetworkRequest(
                                method = runCatching {
                                    NetworkRequest.Method.valueOf(request.method)
                                }.getOrDefault(NetworkRequest.Method.GET),
                                url = url,
                                headers = request.requestHeaders ?: emptyMap(),
                            )
                        )
                    }
                }
                return null
            }
        }

        addJavascriptInterface(bridge, "bridge")
        settings.cacheMode =
            if (runCatching { target.dontCache }.getOrNull() == true) WebSettings.LOAD_NO_CACHE
            else WebSettings.LOAD_DEFAULT
        target.initialUrl.run {
            settings.userAgentString = lowerCaseHeaders["user-agent"] ?: settings.userAgentString
            loadUrl(url, headers)
        }
    }

    suspend fun WebView.evalJS(bridge: Bridge?, js: String) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine {
            bridge?.onResult = it::resume
            bridge?.onError = it::resumeWithException
            val asyncFunction = if (js.startsWith("async function")) js
            else if (js.startsWith("function")) "async $js"
            else {
                it.resumeWithException(Exception("Invalid JS function, must start with async or function"))
                return@suspendCancellableCoroutine
            }
            val newJs = """
            (function() {
                try {
                    const fun = $asyncFunction;
                    fun().then((result) => {
                        bridge.putJsResult(result);
                    }).catch((error) => {
                        bridge.putJsError(error.message || error.toString());
                    });
                } catch (error) {
                    bridge.putJsError(error.message || error.toString());
                }
            })()
            """.trimIndent()
            evaluateJavascript(newJs, null)

            it.invokeOnCancellation {
                evaluateJavascript("javascript:window.stop();", null)
            }
        }
    }

    suspend fun WebView.stop(
        callback: OnBackPressedCallback,
    ) = withContext(Dispatchers.Main) {
        loadUrl("about:blank")
        callback.isEnabled = false
    }

    @Suppress("unused")
    class Bridge {
        var onError: ((Throwable) -> Unit)? = null
        var onResult: ((String?) -> Unit)? = null

        @JavascriptInterface
        fun putJsResult(result: String?) {
            onResult?.invoke(result)
        }

        @JavascriptInterface
        fun putJsError(error: String?) {
            onError?.invoke(Exception(error ?: "Unknown JavaScript error"))
        }
    }

    fun FragmentActivity.onWebViewIntent(
        intent: Intent,
    ) {
        val id = intent.getIntExtra("webViewRequest", -1)
        if (id == -1) return
        val extensionLoader by inject<ExtensionLoader>()
        val webViewClient = extensionLoader.webViewClientFactory
        val wrapper = webViewClient.requests[id] ?: return
        createSnack(Message(getString(R.string.opening_webview_x, wrapper.reason)))
        if (wrapper.showWebView) openFragment<WithAppbar>(null, getBundle(id))
        else supportFragmentManager.commit {
            add<Hidden>(R.id.hiddenWebViewContainer, null, getBundle(id))
        }
    }

    private fun getBundle(id: Int) = Bundle().apply {
        putInt("webViewRequest", id)
    }

    class Hidden : Fragment(R.layout.fragment_webview) {
        private val vm by activityViewModel<ExtensionsViewModel>()
        private val webViewClient by lazy { vm.extensionLoader.webViewClientFactory }
        private val wrapper by lazy {
            val id = requireArguments().getInt("webViewRequest")
            webViewClient.requests[id]
        }
        private val shouldRemove by lazy {
            requireArguments().getBoolean("hidden", true)
        }

        private fun removeSelf() {
            if (shouldRemove) parentFragmentManager.commit(true) { remove(this@Hidden) }
            else parentFragment?.parentFragmentManager?.popBackStack()
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val binding = FragmentWebviewBinding.bind(view)
            val wrapper = wrapper ?: run {
                removeSelf()
                return
            }
            val callback = requireActivity().configure(
                binding.webview,
                binding.progress,
                wrapper.request,
                false
            ) {
                webViewClient.responseFlow.emit(wrapper to it)
                if (it == null) runCatching { removeSelf() }
            }
            if (!shouldRemove) {
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
                applyBackPressCallback()
            }
        }
    }

    class WithAppbar : Fragment(R.layout.fragment_generic_collapsable) {
        private val vm by activityViewModel<ExtensionsViewModel>()
        private val webViewClient by lazy { vm.extensionLoader.webViewClientFactory }
        private val wrapper by lazy {
            val id = requireArguments().getInt("webViewRequest")
            webViewClient.requests[id] ?: throw IllegalStateException("Invalid webview request")
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val binding = FragmentGenericCollapsableBinding.bind(view)
            binding.bind(this)
            binding.toolBar.title = wrapper.extension.name
            wrapper.extension.icon.loadAsCircle(
                binding.extensionIcon, R.drawable.ic_extension_32dp
            ) {
                binding.extensionIcon.setImageDrawable(it)
            }
            addIfNull<Hidden>(R.id.genericFragmentContainer, "webview", arguments?.apply {
                putBoolean("hidden", false)
            })
        }
    }
}
