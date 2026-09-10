package com.rtctek.newsapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import com.rtctek.newsapp.architecture.NewsViewModel
import java.util.Locale

/**
 * In-app article reader (WebView) with text-to-speech, bookmarking, sharing
 * and external-browser hand-off.
 */
class ReadNewsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val viewModel: NewsViewModel by viewModels()

    private lateinit var article: NewsModel
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    private var menu: Menu? = null
    private var isArticleSaved = false

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRate = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incoming = intent.getNewsArticle()
        if (incoming == null) {
            Toast.makeText(this, R.string.invalid_article, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        article = incoming

        setContentView(R.layout.activity_read_news)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.title = article.source ?: getString(R.string.read_news)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressBar = findViewById(R.id.web_progress)
        webView = findViewById(R.id.news_webview)
        setupWebView()

        // Hardware/toolbar back walks WebView history before leaving.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        article.url?.takeIf { it.startsWith("http") }?.let { webView.loadUrl(it) }

        tts = TextToSpeech(this, this)

        viewModel.savedUrls.observe(this) { urls ->
            isArticleSaved = article.url != null && article.url in urls
            updateSaveMenuItem()
        }
        viewModel.message.observe(this) { event ->
            event.getIfNotHandled()?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            // Compatibility mode: allows mixed content only when required,
            // instead of the previous ALWAYS_ALLOW security hole.
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            allowFileAccess = false
            allowContentAccess = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.isVisible = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.isVisible = false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // ── Text-to-speech ──────────────────────────────────────────────

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            ttsReady = false
            return
        }
        val result = tts?.setLanguage(Locale.getDefault())
        ttsReady = result != null &&
            result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!ttsReady) {
            Toast.makeText(this, R.string.tts_unsupported, Toast.LENGTH_LONG).show()
        }
    }

    private fun playNews() {
        if (!ttsReady) {
            Toast.makeText(this, R.string.tts_not_ready, Toast.LENGTH_SHORT).show()
            return
        }
        tts?.setSpeechRate(speechRate)
        tts?.speak(buildSpeechText(), TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    /** Title + description/content, stripped of NewsAPI's "[+1234 chars]" stubs. */
    private fun buildSpeechText(): String {
        val body = article.content
            ?.replace(TRAILING_ELLIPSIS_PATTERN, "")
            ?.takeIf { it.isNotBlank() }
            ?: article.description
            ?: ""
        return "${article.headLine}. $body".trim()
    }

    // ── Options menu ────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_read_news, menu)
        this.menu = menu
        updateSaveMenuItem()
        return true
    }

    private fun updateSaveMenuItem() {
        val item = menu?.findItem(R.id.save_news) ?: return
        item.setIcon(
            if (isArticleSaved) R.drawable.ic_baseline_bookmark_24
            else R.drawable.ic_baseline_bookmark_border_24
        )
        item.setTitle(if (isArticleSaved) R.string.remove_from_saved else R.string.save_for_later)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.play_news -> playNews()
            R.id.stop_news -> tts?.stop()
            R.id.speed_075x -> setSpeed(0.75f)
            R.id.speed_1x -> setSpeed(1.0f)
            R.id.speed_15x -> setSpeed(1.5f)
            R.id.speed_2x -> setSpeed(2.0f)
            R.id.save_news -> viewModel.toggleSaved(article)
            R.id.share_news -> shareArticle()
            R.id.browse_news -> openInBrowser()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setSpeed(rate: Float) {
        speechRate = rate
        playNews()
    }

    private fun shareArticle() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.share_text, article.headLine, article.url.orEmpty()),
            )
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
    }

    private fun openInBrowser() {
        val url = article.url ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    private companion object {
        const val UTTERANCE_ID = "news_speech"
        val TRAILING_ELLIPSIS_PATTERN = Regex("\\s*\\[\\+\\d+ chars?]\\s*$")
    }
}
