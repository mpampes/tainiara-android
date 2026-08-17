package xyz.flixy.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://flixy.xyz/?flixy_tv_app=1";
    private static final String FLIXY_HOST = "flixy.xyz";

    private FrameLayout root;
    private WebView webView;
    private ProgressBar progress;
    private TextView errorView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        enterImmersiveMode();
        buildUi();
        configureWebView();
        if (savedInstanceState != null) webView.restoreState(savedInstanceState); else webView.loadUrl(HOME_URL);
    }

    private void buildUi() {
        root = new FrameLayout(this); root.setBackgroundColor(0xFF050509);
        webView = new WebView(this); webView.setBackgroundColor(0xFF050509); webView.setFocusable(true); webView.setFocusableInTouchMode(true);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); progress.setMax(100);
        errorView = new TextView(this); errorView.setTextColor(0xFFFFFFFF); errorView.setBackgroundColor(0xEE09090F); errorView.setTextSize(22f); errorView.setGravity(android.view.Gravity.CENTER); errorView.setPadding(48,48,48,48); errorView.setText("Το Flixy δεν μπόρεσε να συνδεθεί.\n\nΠάτησε OK για επανάληψη."); errorView.setFocusable(true); errorView.setVisibility(View.GONE);
        errorView.setOnClickListener(v -> retry());
        errorView.setOnKeyListener((v,keyCode,event) -> { if(event.getAction()==KeyEvent.ACTION_UP && (keyCode==KeyEvent.KEYCODE_DPAD_CENTER || keyCode==KeyEvent.KEYCODE_ENTER)){ retry(); return true; } return false; });
        root.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,6); p.gravity=android.view.Gravity.TOP; root.addView(progress,p);
        root.addView(errorView,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    @SuppressWarnings("SetJavaScriptEnabled") private void configureWebView() {
        WebSettings s=webView.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true); s.setMediaPlaybackRequiresUserGesture(false); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false); s.setSupportZoom(false); s.setLoadWithOverviewMode(true); s.setUseWideViewPort(true); s.setCacheMode(WebSettings.LOAD_DEFAULT); s.setUserAgentString(s.getUserAgentString()+" FlixyTV/1.0 AndroidTV");
        CookieManager c=CookieManager.getInstance(); c.setAcceptCookie(true); if(android.os.Build.VERSION.SDK_INT>=21)c.setAcceptThirdPartyCookies(webView,true);
        webView.setWebViewClient(new WebViewClient(){
            @Override public void onPageStarted(WebView v,String u,Bitmap f){progress.setVisibility(View.VISIBLE);errorView.setVisibility(View.GONE);}
            @Override public void onPageFinished(WebView v,String u){progress.setVisibility(View.GONE);injectTvNavigation();v.requestFocus();}
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri uri=r.getUrl();String host=uri.getHost();if(host!=null&&(host.equals(FLIXY_HOST)||host.endsWith("."+FLIXY_HOST)))return false;try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){}return true;}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())showError();}
        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView v,int n){progress.setProgress(n);progress.setVisibility(n>=100?View.GONE:View.VISIBLE);}
            @Override public void onShowCustomView(View v,CustomViewCallback cb){if(customView!=null){cb.onCustomViewHidden();return;}customView=v;customViewCallback=cb;webView.setVisibility(View.GONE);root.addView(customView,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));enterImmersiveMode();}
            @Override public void onHideCustomView(){exitCustomView();}
        });
    }

    private void injectTvNavigation(){String js="javascript:(function(){if(window.__flixyTvReady)return;window.__flixyTvReady=true;document.documentElement.classList.add('flixy-tv-app');var st=document.createElement('style');st.textContent='*:focus{outline:4px solid #fff!important;outline-offset:3px!important;box-shadow:0 0 0 7px rgba(229,9,47,.85)!important;border-radius:8px!important}html{scroll-behavior:smooth}';document.head.appendChild(st);function items(){return Array.from(document.querySelectorAll('a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),[tabindex]:not([tabindex=\\\"-1\\\"])')).filter(function(e){var r=e.getBoundingClientRect(),s=getComputedStyle(e);return r.width>2&&r.height>2&&s.visibility!==\"hidden\"&&s.display!==\"none\";});}function center(r){return{x:r.left+r.width/2,y:r.top+r.height/2};}window.flixyTvMove=function(dir){var a=items();if(!a.length)return;var cur=document.activeElement;if(!a.includes(cur)){a[0].focus();a[0].scrollIntoView({block:\"center\",inline:\"center\"});return;}var c=center(cur.getBoundingClientRect()),best=null,score=1e12;a.forEach(function(el){if(el===cur)return;var p=center(el.getBoundingClientRect()),dx=p.x-c.x,dy=p.y-c.y,ok=(dir===\"left\"&&dx<-4)||(dir===\"right\"&&dx>4)||(dir===\"up\"&&dy<-4)||(dir===\"down\"&&dy>4);if(!ok)return;var primary=(dir===\"left\"||dir===\"right\")?Math.abs(dx):Math.abs(dy),cross=(dir===\"left\"||dir===\"right\")?Math.abs(dy):Math.abs(dx),sc=primary+cross*2.5;if(sc<score){score=sc;best=el;}});if(best){best.focus();best.scrollIntoView({behavior:\"smooth\",block:\"center\",inline:\"center\"});}};window.flixyTvActivate=function(){var e=document.activeElement;if(e&&typeof e.click===\"function\")e.click();};window.flixyTvMedia=function(){var v=document.querySelector('video');if(v){if(v.paused)v.play();else v.pause();}};setTimeout(function(){var a=items();if(a.length&&!a.includes(document.activeElement))a[0].focus();},350);})();";webView.evaluateJavascript(js,null);}

    @Override public boolean onKeyDown(int keyCode,KeyEvent event){if(errorView.getVisibility()==View.VISIBLE)return super.onKeyDown(keyCode,event);switch(keyCode){case KeyEvent.KEYCODE_DPAD_LEFT:runJs("flixyTvMove('left')");return true;case KeyEvent.KEYCODE_DPAD_RIGHT:runJs("flixyTvMove('right')");return true;case KeyEvent.KEYCODE_DPAD_UP:runJs("flixyTvMove('up')");return true;case KeyEvent.KEYCODE_DPAD_DOWN:runJs("flixyTvMove('down')");return true;case KeyEvent.KEYCODE_DPAD_CENTER:case KeyEvent.KEYCODE_ENTER:runJs("flixyTvActivate()");return true;case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:case KeyEvent.KEYCODE_MEDIA_PLAY:case KeyEvent.KEYCODE_MEDIA_PAUSE:runJs("flixyTvMedia()");return true;case KeyEvent.KEYCODE_BACK:handleBack();return true;default:return super.onKeyDown(keyCode,event);}}
    private void runJs(String cmd){webView.evaluateJavascript("javascript:(function(){try{"+cmd+"}catch(e){}})();",null);}
    private void handleBack(){if(customView!=null)exitCustomView();else if(webView.canGoBack())webView.goBack();else finish();}
    private void exitCustomView(){if(customView==null)return;root.removeView(customView);customView=null;webView.setVisibility(View.VISIBLE);if(customViewCallback!=null)customViewCallback.onCustomViewHidden();customViewCallback=null;enterImmersiveMode();}
    private void showError(){progress.setVisibility(View.GONE);errorView.setVisibility(View.VISIBLE);errorView.requestFocus();}
    private void retry(){errorView.setVisibility(View.GONE);webView.reload();webView.requestFocus();}
    private void enterImmersiveMode(){if(android.os.Build.VERSION.SDK_INT>=30){WindowInsetsController controller=getWindow().getInsetsController();if(controller!=null){controller.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(hasFocus)enterImmersiveMode();}
    @Override protected void onSaveInstanceState(Bundle outState){webView.saveState(outState);super.onSaveInstanceState(outState);}
    @Override protected void onPause(){super.onPause();webView.onPause();CookieManager.getInstance().flush();}
    @Override protected void onResume(){super.onResume();webView.onResume();enterImmersiveMode();}
    @Override protected void onDestroy(){if(webView!=null){webView.stopLoading();webView.destroy();}super.onDestroy();}
}
