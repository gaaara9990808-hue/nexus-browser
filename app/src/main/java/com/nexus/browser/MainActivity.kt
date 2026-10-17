package com.nexus.browser

import android.app.*
import android.os.*
import android.content.*
import android.graphics.Color
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.text.InputType
import android.graphics.Typeface
import android.content.res.Configuration
import java.net.URLEncoder

class MainActivity : Activity() {
    private val tabs = mutableListOf<WebView>()
    private var current = -1
    private lateinit var pageArea: FrameLayout
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var tabButton: TextView
    private val bg = Color.rgb(18,24,32)
    private val fg = Color.WHITE
    private val accent = Color.rgb(65,156,255)
    private val prefs by lazy { getSharedPreferences("nexus", MODE_PRIVATE) }
    private val home = "https://www.google.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg; window.navigationBarColor = bg
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(8), dp(5), dp(8), dp(5)); gravity = Gravity.CENTER_VERTICAL }
        address = EditText(this).apply {
            setSingleLine(true); textSize = 15f; setTextColor(fg); setHintTextColor(0xffaabbcc.toInt()); hint = "Pesquisar ou digitar endereço"
            setBackgroundColor(0xff293544.toInt()); setPadding(dp(12),0,dp(12),0)
            imeOptions = EditorInfo.IME_ACTION_GO; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setOnEditorActionListener { _, _, _ -> openInput(); true }
        }
        top.addView(address, LinearLayout.LayoutParams(0,dp(44),1f))
        top.addView(btn("↻") { active()?.reload() }, LinearLayout.LayoutParams(dp(46),dp(44)))
        root.addView(top)
        progress = ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply { max=100; progressTintList=android.content.res.ColorStateList.valueOf(accent) }
        root.addView(progress, LinearLayout.LayoutParams(-1,dp(3)))
        pageArea = FrameLayout(this); root.addView(pageArea, LinearLayout.LayoutParams(-1,0,1f))
        val bottom = LinearLayout(this).apply { gravity=Gravity.CENTER; setPadding(dp(4),dp(2),dp(4),dp(2)) }
        bottom.addView(btn("‹") { if(active()?.canGoBack()==true) active()?.goBack() }, LinearLayout.LayoutParams(0,dp(52),1f))
        bottom.addView(btn("›") { if(active()?.canGoForward()==true) active()?.goForward() }, LinearLayout.LayoutParams(0,dp(52),1f))
        bottom.addView(btn("⌂") { active()?.loadUrl(home) }, LinearLayout.LayoutParams(0,dp(52),1f))
        tabButton = btn("1") { showTabs() }; bottom.addView(tabButton, LinearLayout.LayoutParams(0,dp(52),1f))
        bottom.addView(btn("⋮") { showMenu() }, LinearLayout.LayoutParams(0,dp(52),1f))
        root.addView(bottom); setContentView(root)
        newTab(intent?.dataString?.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: home)
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun btn(label:String, action:()->Unit)=TextView(this).apply {
        text=label; textSize=25f; setTextColor(fg); gravity=Gravity.CENTER; setOnClickListener { action() }
        setBackgroundResource(android.R.drawable.list_selector_background)
    }
    private fun active()=tabs.getOrNull(current)
    private fun openInput() {
        val raw=address.text.toString().trim(); if(raw.isEmpty()) return
        val url = when {
            raw.startsWith("https://") || raw.startsWith("http://") -> raw
            !raw.contains(" ") && raw.contains(".") -> "https://$raw"
            else -> "https://www.google.com/search?q="+URLEncoder.encode(raw,"UTF-8")
        }
        active()?.loadUrl(url)
        (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(address.windowToken,0)
        address.clearFocus()
    }
    private fun newTab(url:String) {
        val w=WebView(this)
        w.setBackgroundColor(Color.WHITE)
        w.settings.apply {
            javaScriptEnabled=true; domStorageEnabled=true; loadWithOverviewMode=true; useWideViewPort=true
            builtInZoomControls=true; displayZoomControls=false; allowFileAccess=false; allowContentAccess=true
            javaScriptCanOpenWindowsAutomatically=false; setSupportMultipleWindows(false)
            mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(w,false)
        w.webViewClient=object:WebViewClient() {
            override fun shouldOverrideUrlLoading(view:WebView, request:WebResourceRequest):Boolean {
                val uri=request.url; val scheme=uri.scheme
                if(scheme=="http" || scheme=="https") return false
                if(request.isForMainFrame) try { startActivity(Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE)) } catch (_:Exception) { toast("Não há app para abrir este link") }
                return true
            }
            override fun onPageStarted(view:WebView,url:String?,favicon:Bitmap?) { if(view===active()) { address.setText(url ?: ""); progress.visibility=View.VISIBLE } }
            override fun onPageFinished(view:WebView,url:String?) { if(view===active()) { address.setText(url ?: ""); progress.visibility=View.GONE }; if(!url.isNullOrEmpty() && url.startsWith("http")) prefs.edit().putString("last",url).apply() }
            override fun onReceivedSslError(view:WebView, handler:SslErrorHandler, error:android.net.http.SslError) { handler.cancel(); toast("Conexão insegura bloqueada") }
            override fun onRenderProcessGone(view:WebView, detail:RenderProcessGoneDetail):Boolean {
                val i=tabs.indexOf(view); if(i<0) return true
                if(view.parent===pageArea) pageArea.removeView(view)
                tabs.removeAt(i); view.destroy()
                if(tabs.isEmpty()) { current=-1; newTab(home) } else switchTab(i.coerceAtMost(tabs.lastIndex))
                toast("Aba recuperada após falha do site")
                return true
            }
        }
        w.webChromeClient=object:WebChromeClient() {
            override fun onProgressChanged(view:WebView,p:Int) { if(view===active()) { progress.progress=p; progress.visibility=if(p>=100) View.GONE else View.VISIBLE } }
            override fun onShowFileChooser(webView:WebView, filePathCallback:ValueCallback<Array<Uri>>, fileChooserParams:FileChooserParams):Boolean {
                chooserCallback?.onReceiveValue(null); chooserCallback=filePathCallback
                return try { startActivityForResult(fileChooserParams.createIntent(), 42); true } catch (_:Exception) { chooserCallback=null; filePathCallback.onReceiveValue(null); false }
            }
        }
        w.setDownloadListener { url,userAgent,contentDisposition,mimeType,_ ->
            if(!url.startsWith("https://") && !url.startsWith("http://")) { toast("Download não suportado"); return@setDownloadListener }
            try {
                val request=DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType); request.addRequestHeader("User-Agent",userAgent ?: w.settings.userAgentString)
                val cookie=CookieManager.getInstance().getCookie(url); if(!cookie.isNullOrEmpty()) request.addRequestHeader("Cookie",cookie)
                request.setTitle(URLUtil.guessFileName(url,contentDisposition,mimeType)); request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(url,contentDisposition,mimeType))
                (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request); toast("Download iniciado")
            } catch (_:Exception) { toast("Falha ao iniciar download") }
        }
        tabs.add(w); switchTab(tabs.lastIndex); w.loadUrl(url)
    }
    private var chooserCallback:ValueCallback<Array<Uri>>?=null
    @Deprecated("Android file chooser callback")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==42) { chooserCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode,data)); chooserCallback=null }
    }
    private fun switchTab(i:Int) {
        if(i !in tabs.indices) return
        active()?.onPause(); pageArea.removeAllViews(); current=i
        val w=tabs[i]; (w.parent as? android.view.ViewGroup)?.removeView(w)
        pageArea.addView(w,FrameLayout.LayoutParams(-1,-1)); w.onResume()
        address.setText(w.url ?: ""); tabButton.text="${tabs.size}"; progress.visibility=View.GONE
    }
    private fun showTabs() {
        val names=tabs.mapIndexed { i,w -> "${i+1}. ${w.title ?: w.url ?: "Nova aba"}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Abas (${tabs.size})").setItems(names) { _,which -> switchTab(which) }
            .setPositiveButton("Nova aba") { _,_ -> newTab(home) }
            .setNeutralButton("Fechar atual") { _,_ -> closeTab() }
            .setNegativeButton("Cancelar",null).show()
    }
    private fun closeTab() {
        if(current<0)return
        val i=current; val w=tabs.removeAt(i); pageArea.removeView(w); w.stopLoading(); w.destroy()
        if(tabs.isEmpty()) { current=-1; newTab(home) } else { current=-1; switchTab(i.coerceAtMost(tabs.lastIndex)) }
    }
    private fun showMenu() {
        val options=arrayOf("Adicionar favorito","Favoritos","Compartilhar página","Abrir downloads","Modo desktop","Limpar dados de navegação","Nova aba","Fechar aba")
        AlertDialog.Builder(this).setTitle("NEXUS Browser").setItems(options) { _,i -> when(i) {
            0 -> { val u=active()?.url ?: return@setItems; val title=active()?.title ?: u; val old=prefs.getString("bookmarks","") ?: ""; if(!old.lines().any { it.substringAfter('|',"")==u }) prefs.edit().putString("bookmarks",old+title.replace("|"," ").replace("\n"," ")+"|"+u+"\n").apply(); toast("Favorito salvo") }
            1 -> showBookmarks()
            2 -> active()?.url?.let { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,it) },"Compartilhar")) }
            3 -> try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) } catch (_:Exception) { toast("Downloads indisponíveis") }
            4 -> { val w=active() ?: return@setItems; val desktop=!w.settings.userAgentString.contains("X11; Linux x86_64"); w.settings.userAgentString=if(desktop) "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36" else null; w.reload(); toast(if(desktop) "Modo desktop" else "Modo celular") }
            5 -> AlertDialog.Builder(this).setTitle("Limpar dados?").setMessage("Remove cache, cookies e histórico local do WebView.").setPositiveButton("Limpar") { _,_ -> tabs.forEach { it.clearHistory(); it.clearCache(true) }; CookieManager.getInstance().removeAllCookies(null); WebStorage.getInstance().deleteAllData(); toast("Dados removidos") }.setNegativeButton("Cancelar",null).show()
            6 -> newTab(home)
            7 -> closeTab()
        } }.show()
    }
    private fun showBookmarks() {
        val entries=(prefs.getString("bookmarks","") ?: "").lines().filter { it.contains('|') }
        if(entries.isEmpty()) { toast("Nenhum favorito salvo"); return }
        AlertDialog.Builder(this).setTitle("Favoritos").setItems(entries.map { it.substringBefore('|') }.toTypedArray()) { _,i -> active()?.loadUrl(entries[i].substringAfter('|')) }
            .setNeutralButton("Apagar favoritos") { _,_ -> AlertDialog.Builder(this).setMessage("Apagar todos os favoritos?").setPositiveButton("Apagar") { _,_ -> prefs.edit().remove("bookmarks").apply() }.setNegativeButton("Cancelar",null).show() }
            .setNegativeButton("Fechar",null).show()
    }
    private fun toast(msg:String)=Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
    override fun onBackPressed() { if(active()?.canGoBack()==true) active()?.goBack() else AlertDialog.Builder(this).setMessage("Sair do NEXUS Browser?").setPositiveButton("Sair") { _,_ -> finish() }.setNegativeButton("Cancelar",null).show() }
    override fun onDestroy() { tabs.forEach { (it.parent as? android.view.ViewGroup)?.removeView(it); it.destroy() }; tabs.clear(); super.onDestroy() }
}
