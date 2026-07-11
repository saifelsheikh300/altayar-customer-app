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
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebChromeClient;

/**
 * تمت إضافة هذا الملف بواسطة Claude فوق قالب Capacitor الافتراضي (كان فاضي أصلاً).
 *
 * فيه حاجتين هنا:
 * 1) السبلاش (زي ما هي زي تطبيق المندوب بالظبط): شاشة برتقالي واللوجو في النص،
 *    فاضلة طول ما موقع العميل لسه بيحمّل، وبتختفي أول ما المحتوى يخلص تحميل.
 * 2) إذن المايك: لأن الموقع بيستخدم navigator.mediaDevices.getUserMedia للتسجيل الصوتي،
 *    والـ WebView الافتراضي في أندرويد مش بيطلب الإذن من نفسه لازم نتحكم فيه يدويًا هنا.
 *    الإذن بيتطلب أول لحظة المستخدم يدوس على زرار المايك في الموقع (مش من أول ما يفتح التطبيق).
 *
 * ملحوظة: مفيش أي لمس لـ WebViewClient بتاع كباسيتور، فبريدج البلجنز شغال عادي.
 */
public class MainActivity extends BridgeActivity {

    private static final int MIN_DISPLAY_MS = 400;   // أقل مدة ظهور حتى لو الموقع حمّل فوراً
    private static final int MAX_DISPLAY_MS = 7000;   // أقصى مدة أمان لو النت بطيء أو مقطوع
    private static final int POLL_INTERVAL_MS = 100;
    private static final int MIC_PERMISSION_CODE = 5001;

    private PermissionRequest pendingWebRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showSplashUntilLoaded();
        setupWebChromeClientForMic();
    }

    private void showSplashUntilLoaded() {
        final float density = getResources().getDisplayMetrics().density;
        final long startTime = System.currentTimeMillis();

        final FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#FD5003")); // نفس لون البراند

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

        webView.setWebChromeClient(new BridgeWebChromeClient(getBridge()) {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    boolean needsAudio = false;
                    for (String res : request.getResources()) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res)) {
                            needsAudio = true;
                        }
                    }
                    if (!needsAudio) {
                        request.deny();
                        return;
                    }

                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {
                        request.grant(request.getResources());
                    } else {
                        pendingWebRequest = request;
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
                    }
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_CODE && pendingWebRequest != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingWebRequest.grant(pendingWebRequest.getResources());
            } else {
                pendingWebRequest.deny();
            }
            pendingWebRequest = null;
        }
    }
}
