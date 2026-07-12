package com.altayar.customer;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;

/**
 * تمت إضافة هذا الملف بواسطة Claude فوق قالب Capacitor الافتراضي (كان فاضي أصلاً).
 *
 * فيه حاجتين هنا:
 * 1) السبلاش (زي ما هي زي تطبيق المندوب بالظبط).
 * 2) إذن المايك: بنطلب إذن RECORD_AUDIO من أول ما التطبيق يفتح (مش وقت الضغط على
 *    زرار المايك)، عشان لما الموقع يطلب الوصول للمايك (onPermissionRequest) نكون
 *    جاهزين نوافق فورًا من غير أي تأخير ممكن يبوّظ الـ WebView.
 */
public class MainActivity extends BridgeActivity {

    private static final int MIN_DISPLAY_MS = 400;
    private static final int MAX_DISPLAY_MS = 7000;
    private static final int POLL_INTERVAL_MS = 100;
    private static final int MIC_PERMISSION_CODE = 5001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // اطلب إذن المايك فورًا من أول ما التطبيق يفتح
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
        }

        showSplashUntilLoaded();
        setupWebChromeClientForMic();
        disableWebViewCache();
    }

    private void disableWebViewCache() {
        WebView webView = getBridge() != null ? getBridge().getWebView() : null;
        if (webView == null) return;
        webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);
        webView.clearCache(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        // نعيد ضبط WebChromeClient تاني هنا كإجراء احترازي، لاحتمال إن كباسيتور
        // بيرجعه لإعداداته الافتراضية بعد onCreate في بعض الحالات
        setupWebChromeClientForMic();
        disableWebViewCache();
    }

    private void showSplashUntilLoaded() {
        final float density = getResources().getDisplayMetrics().density;
        final long startTime = System.currentTimeMillis();

        final FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#FD5003"));

        final ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.altayar_splash_logo);
        FrameLayout.LayoutParams logoParams = new FrameLayout.LayoutParams(
                (int) (170 * density), (int) (170 * density));
        logoParams.gravity = Gravity.CENTER;
        logo.setLayoutParams(logoParams);
        logo.setAlpha(0f);
        overlay.addView(logo);

        final ViewGroup root = findViewById(android.R.id.content);
        root.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
        fadeIn.setDuration(200);
        fadeIn.start();

        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable[] checker = new Runnable[1];
        checker[0] = () -> {
            WebView webView = getBridge() != null ? getBridge().getWebView() : null;
            boolean loaded = webView != null && webView.getProgress() >= 100;
            long elapsed = System.currentTimeMillis() - startTime;

            if ((loaded && elapsed >= MIN_DISPLAY_MS) || elapsed >= MAX_DISPLAY_MS) {
                ObjectAnimator fadeOut = ObjectAnimator.ofFloat(overlay, "alpha", 1f, 0f);
                fadeOut.setDuration(450);
                fadeOut.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        root.removeView(overlay);
                    }
                });
                fadeOut.start();
            } else {
                handler.postDelayed(checker[0], POLL_INTERVAL_MS);
            }
        };
        handler.postDelayed(checker[0], POLL_INTERVAL_MS);
    }

    private void setupWebChromeClientForMic() {
        final WebView webView = getBridge() != null ? getBridge().getWebView() : null;
        if (webView == null) return;

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "📢 وصل طلب إذن المايك من الموقع", Toast.LENGTH_LONG).show();
                    request.grant(request.getResources());
                });
            }
        });
    }
}
