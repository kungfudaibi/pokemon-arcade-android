package net.pokerogue.livewrapper;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME = "https://pokerogue.net/";
    private static final int FILE_REQUEST = 1001;
    private FrameLayout container;
    private WebView webView;
    private android.webkit.ValueCallback<Uri[]> fileCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(attributes);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        enterImmersiveMode();
        container = new FrameLayout(this);
        setContentView(container);
        createWebView();
        webView.loadUrl(HOME);
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }

    private void createWebView() {
        webView = new WebView(this);
        container.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                if (isOfficialPage(request.getUrl())) return false;
                openExternal(request.getUrl());
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                Log.i("PokerogueLive", "Loaded " + url);
                installLandscapeFill(view);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    Log.e("PokerogueLive", "Page load failed: " + error.getDescription());
                    Toast.makeText(MainActivity.this, "官网加载失败，请检查网络后重试", Toast.LENGTH_LONG).show();
                }
            }
            @Override public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                container.removeView(view);
                view.destroy();
                fileCallback = null;
                createWebView();
                webView.loadUrl(HOME);
                Toast.makeText(MainActivity.this, "网页进程已重启，请从存档继续", Toast.LENGTH_LONG).show();
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view,
                    android.webkit.ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "找不到文件选择器", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        webView.setDownloadListener((url, userAgent, disposition, mimeType, length) -> {
            if (url.startsWith("https://")) openExternal(Uri.parse(url));
            else Toast.makeText(this, "此版本暂不支持网页内导出文件", Toast.LENGTH_LONG).show();
        });
    }

    private void installLandscapeFill(WebView view) {
        String script = "(function(){"
                + "if(window.__pokerogueLiveFill)return;window.__pokerogueLiveFill=true;"
                + "var observer=('ResizeObserver'in window)?new ResizeObserver(function(){setTimeout(fill,80)}):null;"
                + "function fill(){"
                + "var app=document.getElementById('app');if(!app)return false;"
                + "var canvas=app.querySelector('canvas');if(!canvas)return false;"
                + "app.style.transform='';"
                + "if(innerWidth<=innerHeight)return true;"
                + "var width=canvas.getBoundingClientRect().width;"
                + "if(width<1)return false;"
                + "var scale=innerWidth/width;"
                + "app.style.transformOrigin='center center';"
                + "app.style.transform='scaleX('+scale+')';"
                + "document.documentElement.style.overflow='hidden';"
                + "document.body.style.overflow='hidden';"
                + "return true;"
                + "}"
                + "var timer=setInterval(function(){if(fill()){clearInterval(timer);"
                + "var canvas=document.querySelector('#app canvas');if(canvas&&observer)observer.observe(canvas);"
                + "}},300);"
                + "window.addEventListener('resize',function(){setTimeout(fill,200)});"
                + "})();";
        view.evaluateJavascript(script, null);
    }

    private boolean isOfficialPage(Uri uri) {
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme()) &&
                ("pokerogue.net".equalsIgnoreCase(host) || "www.pokerogue.net".equalsIgnoreCase(host));
    }

    private void openExternal(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException e) {
            Toast.makeText(this, "找不到可打开链接的应用", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }
        if (webView != null) {
            container.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
