package com.rkdesain.marmut;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebView.WebViewTransport;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.http.SslError;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Marmut: a small WebView shell for the user's website.
 * The first tab is permanent; temporary popup tabs can be closed independently.
 */
public class MainActivity extends Activity {
    private static final String HOME_URL = "https://jembot0mlungkertempekmambu.netlify.app/";
    private static final String HOME_HOST = "jembot0mlungkertempekmambu.netlify.app";
    // Limit simultaneous WebViews to reduce memory use on phones.
    private static final int MAX_TABS = 4;

    private LinearLayout root;
    private LinearLayout tabsStrip;
    private HorizontalScrollView tabsScroll;
    private FrameLayout webContainer;
    private ProgressBar progressBar;
    private TextView appTitle;
    private BrowserTab homeTab;
    private BrowserTab currentTab;
    private final List<BrowserTab> tabs = new ArrayList<>();
    private boolean isFinishingCleanup = false;

    private static class BrowserTab {
        final boolean isHome;
        WebView webView;
        String title;

        BrowserTab(boolean isHome, String title) {
            this.isHome = isHome;
            this.title = title;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(7, 87, 200));
        window.setNavigationBarColor(Color.rgb(7, 87, 200));

        buildLayout();

        homeTab = new BrowserTab(true, getString(R.string.home_tab));
        homeTab.webView = createWebView(homeTab);
        tabs.add(homeTab);
        addWebViewToContainer(homeTab.webView, View.INVISIBLE);
        currentTab = homeTab;
        switchTo(homeTab);
        homeTab.webView.loadUrl(HOME_URL);
    }

    private void buildLayout() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // Compact app bar with the supplied marmut icon on the left.
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(9), 0, dp(6), 0);
        toolbar.setBackgroundColor(Color.rgb(8, 124, 255));
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setContentDescription(getString(R.string.app_name));
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        toolbar.addView(logo, new LinearLayout.LayoutParams(dp(39), dp(39)));

        appTitle = new TextView(this);
        appTitle.setText(getString(R.string.app_name));
        appTitle.setTextColor(Color.WHITE);
        appTitle.setTextSize(18);
        appTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        appTitle.setSingleLine(true);
        appTitle.setPadding(dp(9), 0, dp(8), 0);
        toolbar.addView(appTitle, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView homeButton = makeToolbarButton("⌂", getString(R.string.go_home));
        homeButton.setOnClickListener(v -> switchTo(homeTab));
        toolbar.addView(homeButton, new LinearLayout.LayoutParams(dp(42), dp(44)));

        TextView reloadButton = makeToolbarButton("↻", getString(R.string.reload));
        reloadButton.setOnClickListener(v -> {
            if (currentTab != null && currentTab.webView != null) currentTab.webView.reload();
        });
        toolbar.addView(reloadButton, new LinearLayout.LayoutParams(dp(42), dp(44)));

        tabsScroll = new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        tabsScroll.setFillViewport(false);
        tabsScroll.setBackgroundColor(Color.rgb(238, 245, 255));
        root.addView(tabsScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(43)));

        tabsStrip = new LinearLayout(this);
        tabsStrip.setOrientation(LinearLayout.HORIZONTAL);
        tabsStrip.setGravity(Gravity.CENTER_VERTICAL);
        tabsStrip.setPadding(dp(6), dp(4), dp(6), dp(4));
        tabsScroll.addView(tabsStrip, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= 21) {
            progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(8, 124, 255)));
            progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(225, 237, 255)));
        }
        root.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        webContainer = new FrameLayout(this);
        webContainer.setBackgroundColor(Color.WHITE);
        root.addView(webContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private TextView makeToolbarButton(String text, String description) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(27);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(description);
        button.setBackground(makeRippleBackground(Color.TRANSPARENT, Color.rgb(30, 145, 255), dp(10)));
        button.setFocusable(true);
        button.setClickable(true);
        return button;
    }

    private WebView createWebView(BrowserTab tab) {
        WebView webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return routeNavigation(tab, request.getUrl(), request.isForMainFrame());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return routeNavigation(tab, Uri.parse(url), true);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (tab == currentTab) {
                    progressBar.setProgress(5);
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (tab == currentTab) {
                    progressBar.setProgress(100);
                    progressBar.setVisibility(View.GONE);
                }
                if (tab.isHome && isHomeUrl(url)) {
                    tab.title = getString(R.string.home_tab);
                    refreshTabs();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && tab == currentTab) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this,
                            "Halaman gagal dimuat. Periksa internet lalu tekan muat ulang.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (tab == currentTab) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Never ignore certificate errors; cancel the unsafe connection.
                handler.cancel();
                if (tab == currentTab) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Sambungan aman gagal diverifikasi.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                handleRendererGone(tab, view);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, Message resultMsg) {
                if (isFinishingCleanup || tabs.size() >= MAX_TABS) {
                    showTabLimitMessage();
                    return false;
                }
                BrowserTab newTab = createPopupTab(null);
                if (newTab == null) return false;
                Object rawTransport = resultMsg.obj;
                if (!(rawTransport instanceof WebViewTransport)) {
                    closeTab(newTab);
                    return false;
                }
                WebViewTransport transport = (WebViewTransport) rawTransport;
                transport.setWebView(newTab.webView);
                resultMsg.sendToTarget();
                switchTo(newTab);
                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                BrowserTab match = findTab(window);
                if (match != null && !match.isHome) closeTab(match);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (tab == currentTab && progressBar != null) {
                    progressBar.setProgress(newProgress);
                    progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (title != null && !title.trim().isEmpty() && !"about:blank".equalsIgnoreCase(title.trim())) {
                    tab.title = title.trim();
                    refreshTabs();
                }
            }
        });
        return webView;
    }

    private boolean routeNavigation(BrowserTab sourceTab, Uri uri, boolean mainFrame) {
        if (uri == null) return true;
        String scheme = uri.getScheme();
        if (scheme == null) return true;

        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            // Keep the user's own site in the permanent home tab. Top-level links away
            // from that site open a temporary tab instead of replacing the home page.
            if (mainFrame && sourceTab.isHome && !isHomeHost(uri.getHost())) {
                BrowserTab external = createPopupTab(uri.toString());
                if (external == null) {
                    showTabLimitMessage();
                    return true;
                }
                switchTo(external);
                external.webView.loadUrl(uri.toString());
                return true;
            }
            return false;
        }

        // Only hand off common user-facing schemes. Do not execute arbitrary intent:// URLs.
        if ("mailto".equalsIgnoreCase(scheme) || "tel".equalsIgnoreCase(scheme)) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            } catch (Exception ignored) {
                Toast.makeText(this, "Tidak ada aplikasi yang bisa membuka tautan ini.", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    private BrowserTab createPopupTab(String initialUrl) {
        if (tabs.size() >= MAX_TABS || isFinishingCleanup) return null;
        BrowserTab tab = new BrowserTab(false, getString(R.string.new_tab));
        tab.webView = createWebView(tab);
        tabs.add(tab);
        addWebViewToContainer(tab.webView, View.INVISIBLE);
        refreshTabs();
        if (initialUrl != null && !initialUrl.trim().isEmpty()) {
            tab.webView.loadUrl(initialUrl);
        }
        return tab;
    }

    private void addWebViewToContainer(WebView webView, int visibility) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        webView.setVisibility(visibility);
        webContainer.addView(webView, params);
    }

    private void switchTo(BrowserTab selected) {
        if (selected == null || !tabs.contains(selected)) return;
        currentTab = selected;
        for (BrowserTab tab : tabs) {
            if (tab.webView != null) tab.webView.setVisibility(tab == selected ? View.VISIBLE : View.INVISIBLE);
        }
        appTitle.setText(selected.isHome ? getString(R.string.app_name) : "Marmut · " + safeTitle(selected.title));
        if (selected.webView != null) selected.webView.requestFocus();
        refreshTabs();
    }

    private void refreshTabs() {
        if (tabsStrip == null) return;
        tabsStrip.removeAllViews();
        for (BrowserTab tab : tabs) {
            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(11), 0, dp(7), 0);
            chip.setBackground(makeRippleBackground(
                    tab == currentTab ? Color.rgb(212, 231, 255) : Color.WHITE,
                    Color.rgb(221, 236, 255), dp(13)));
            chip.setElevation(dp(1));
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(33));
            chipParams.setMargins(dp(3), 0, dp(3), 0);

            TextView title = new TextView(this);
            title.setText(tab.isHome ? getString(R.string.home_tab) : safeTitle(tab.title));
            title.setTextColor(Color.rgb(23, 50, 79));
            title.setTextSize(12.5f);
            title.setSingleLine(true);
            title.setMaxWidth(dp(142));
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.addView(title, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

            if (!tab.isHome) {
                TextView close = new TextView(this);
                close.setText("×");
                close.setTextSize(20);
                close.setTextColor(Color.rgb(83, 105, 130));
                close.setGravity(Gravity.CENTER);
                close.setContentDescription(getString(R.string.close_tab));
                LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(25), dp(30));
                closeParams.setMargins(dp(4), 0, 0, 0);
                chip.addView(close, closeParams);
                close.setOnClickListener(v -> closeTab(tab));
            }
            chip.setOnClickListener(v -> switchTo(tab));
            tabsStrip.addView(chip, chipParams);
        }
    }

    private void closeTab(BrowserTab tab) {
        if (tab == null || tab.isHome || !tabs.contains(tab)) return;
        boolean wasCurrent = tab == currentTab;
        tabs.remove(tab);
        destroyWebView(tab.webView);
        tab.webView = null;
        if (wasCurrent) {
            switchTo(homeTab);
        } else {
            refreshTabs();
        }
    }

    private void destroyWebView(WebView webView) {
        if (webView == null) return;
        try {
            webView.stopLoading();
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) parent.removeView(webView);
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        } catch (Exception ignored) {
            // Cleanup should not crash the app when the renderer has already exited.
        }
    }

    private void handleRendererGone(BrowserTab tab, WebView crashedView) {
        if (crashedView == null || isFinishingCleanup) return;
        if (tab == null || tab.webView != crashedView) {
            destroyWebView(crashedView);
            return;
        }
        boolean wasCurrent = tab == currentTab;
        if (!tab.isHome) {
            closeTab(tab);
            Toast.makeText(this, "Tab ditutup karena halaman berhenti merespons.", Toast.LENGTH_LONG).show();
            return;
        }

        ViewGroup parent = (ViewGroup) crashedView.getParent();
        if (parent != null) parent.removeView(crashedView);
        try {
            crashedView.stopLoading();
            crashedView.setWebChromeClient(null);
            crashedView.setWebViewClient(null);
            crashedView.destroy();
        } catch (Exception ignored) { }

        tab.webView = createWebView(tab);
        addWebViewToContainer(tab.webView, wasCurrent ? View.VISIBLE : View.INVISIBLE);
        if (wasCurrent) switchTo(tab);
        tab.webView.loadUrl(HOME_URL);
        Toast.makeText(this, "Halaman utama dimuat ulang untuk memulihkan aplikasi.", Toast.LENGTH_LONG).show();
    }

    private BrowserTab findTab(WebView webView) {
        for (BrowserTab tab : tabs) if (tab.webView == webView) return tab;
        return null;
    }

    private void showTabLimitMessage() {
        Toast.makeText(this, "Maksimal 4 tab agar aplikasi tetap ringan. Tutup tab yang tidak dipakai.", Toast.LENGTH_LONG).show();
    }

    private boolean isHomeUrl(String value) {
        try { return isHomeHost(Uri.parse(value).getHost()); }
        catch (Exception ignored) { return false; }
    }

    private boolean isHomeHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        return h.equals(HOME_HOST) || h.endsWith("." + HOME_HOST);
    }

    private String safeTitle(String title) {
        if (title == null || title.trim().isEmpty() || "about:blank".equalsIgnoreCase(title.trim())) {
            return getString(R.string.new_tab);
        }
        return title.trim();
    }

    private GradientDrawable makeRippleBackground(int baseColor, int pressedColor, int radiusPx) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(baseColor);
        shape.setCornerRadius(radiusPx);
        return shape;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onBackPressed() {
        if (currentTab != null && currentTab != homeTab) {
            // Required behavior: Android Back returns directly to the permanent home tab.
            switchTo(homeTab);
            return;
        }
        if (homeTab != null && homeTab.webView != null && homeTab.webView.canGoBack()) {
            homeTab.webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // WebViews remain alive across rotation; the container resizes automatically.
    }

    @Override
    protected void onDestroy() {
        isFinishingCleanup = true;
        for (BrowserTab tab : new ArrayList<>(tabs)) {
            destroyWebView(tab.webView);
            tab.webView = null;
        }
        tabs.clear();
        super.onDestroy();
    }
}
