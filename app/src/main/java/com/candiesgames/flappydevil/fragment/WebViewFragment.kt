package com.candiesgames.flappydevil.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.candiesgames.flappydevil.R
import com.candiesgames.flappydevil.view_model.MainViewModel
import com.candiesgames.flappydevil.view_model.MainViewModelFactory

class WebViewFragment : Fragment() {

    private lateinit var mainVM: MainViewModel

    private var fileChooserValueCallback: ValueCallback<Array<Uri>>? = null
    private var fileChooserResultLauncher = createFileChooserResultLauncher()

    private var childView: WebView? = null
    private var transport: WebView.WebViewTransport? = null

    private lateinit var container: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var webView: WebView

    override fun onCreateView(
        inflater: LayoutInflater, group: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        mainVM = ViewModelProvider(this, MainViewModelFactory(requireContext()))[MainViewModel::class.java]

        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        return createWebView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBackPressed()
        hideSystemBar()

        initWebView()
    }

    private fun createWebView() = requireContext().let {
        container = FrameLayout(it).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        progressBar = ProgressBar(it).apply {
            val size = dpToPx(42f)
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }
        }

        progressBar.indeterminateDrawable
            .setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.black),
                PorterDuff.Mode.SRC_IN
            )

        webView = WebView(it).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }

        listOf(webView, progressBar).forEach { v -> container.addView(v) }

        container
    }

    private fun initWebView() {
        webView.apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setWebViewSettings()
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = myWebViewClient

            webChromeClient = myWebChromeClient

            loadUrl("http://html5test.com")

        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun WebView.setWebViewSettings() {
        this.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            if (mimeType.equals("application/vnd.android.package-archive", ignoreCase = true)) {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading APK")
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))

                val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)
            }
        }

        settings.apply {
            javaScriptCanOpenWindowsAutomatically = true
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            allowContentAccess = true
            allowFileAccess = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportMultipleWindows(true)
            setSupportZoom(true)
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        if (Build.VERSION.SDK_INT >= 21) {
            cookieManager.setAcceptThirdPartyCookies(this, true)
        }

        this.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
    }

    private val myWebViewClient = object : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest
        ): Boolean {
            val urlLoading = request.url.toString()

            if (urlLoading.contains("instagram")
                || urlLoading.contains("viber")
                || urlLoading.contains("telegram")
            ) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlLoading))
                startActivity(intent)
            }
            return false
        }

        override fun onPageFinished(layout: WebView?, webURL: String) {
            super.onPageFinished(layout, webURL)

            webView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }

    }

    private val myWebChromeClient = object : WebChromeClient() {

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            try {
                fileChooserValueCallback = filePathCallback

                fileChooserResultLauncher.launch(fileChooserParams?.createIntent())
            } catch (e: ActivityNotFoundException) {
                // You may handle "No activity found to handle intent" error
            }
            return true
        }

        var fullscreen: View? = null


        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            webView.visibility = View.GONE

            if (fullscreen != null) {
                (requireActivity().window.decorView as FrameLayout).removeView(fullscreen)
                requireActivity().window.decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
                fullscreen = null
            }

            fullscreen = view
            (requireActivity().window.decorView as FrameLayout).addView(
                fullscreen,
                FrameLayout.LayoutParams(-1, -1)
            )
            requireActivity().window.decorView
                .setSystemUiVisibility(3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            super.onShowCustomView(view, callback)
        }

        override fun onHideCustomView() {
            fullscreen!!.visibility = View.GONE
            webView.visibility = View.VISIBLE

            (requireActivity().window
                .decorView as FrameLayout).removeView(fullscreen)
            requireActivity().window
                .decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )

            fullscreen = null
            super.onHideCustomView()
        }

        @SuppressLint("SetJavaScriptEnabled")
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            childView = WebView(requireContext())
            childView!!.settings.apply {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
            }

            childView!!.webChromeClient = this
            childView!!.webViewClient = WebViewClient()

            childView!!.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT
            )
            (requireActivity().window.decorView as FrameLayout).addView(childView)

            transport = resultMsg!!.obj as WebView.WebViewTransport
            transport!!.webView = childView
            resultMsg.sendToTarget()
            return true

        }

        override fun onCloseWindow(window: WebView?) {
            super.onCloseWindow(window)
            childView = null
            transport = null
            (requireActivity().window.decorView as FrameLayout).removeView(window)
        }

    }

    private fun createFileChooserResultLauncher(): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                fileChooserValueCallback?.onReceiveValue(arrayOf(Uri.parse(it?.data?.dataString)));
            } else {
                fileChooserValueCallback?.onReceiveValue(null)
            }
        }
    }

    private fun hideSystemBar() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)

        WindowInsetsControllerCompat(
            requireActivity().window,
            requireActivity().window.decorView.rootView
        ).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            requireActivity().window.decorView.setOnSystemUiVisibilityChangeListener {
                if (it != 0 && View.SYSTEM_UI_FLAG_FULLSCREEN != 0) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }

                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        }
    }

    private fun initBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (childView != null) {
                        (requireActivity().window.decorView as FrameLayout).removeView(transport!!.webView)
                        transport = null
                        childView = null
                    } else {
                        webView.goBack()
                    }
                }
            })
    }

    private fun dpToPx(dp: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (dp * scale).toInt()
    }

}
