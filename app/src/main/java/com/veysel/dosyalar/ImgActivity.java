package com.veysel.dosyalar;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.net.Uri;
import android.os.Build;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

public class ImgActivity extends Activity {
    static ArrayList<File> list;
    static int idx;

    Uri one;
    Z z;
    TextView info;
    Handler h = new Handler();
    boolean slide;
    int token;
    final float[] ZS = {1f, 2f, 3f, 4f};
    int zi;
    final Runnable hide = () -> info.setVisibility(View.GONE);
    final Runnable tick = new Runnable() {
        public void run() {
            if (!slide) return;
            go(1);
            h.postDelayed(this, 4000);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        one = getIntent().getData();
        if (one == null && (list == null || list.isEmpty())) { finish(); return; }
        FrameLayout r = new FrameLayout(this);
        r.setBackgroundColor(Color.BLACK);
        z = new Z();
        r.addView(z, new FrameLayout.LayoutParams(-1, -1));
        info = new TextView(this);
        info.setTextColor(Color.WHITE); info.setTextSize(15);
        info.setBackgroundColor(0xAA000000);
        info.setPadding(U.dp(this, 16), U.dp(this, 8), U.dp(this, 16), U.dp(this, 8));
        r.addView(info, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        setContentView(r);
        load();
    }

    void load() {
        final int t = ++token;
        final File f = one == null ? list.get(idx) : null;
        zi = 0;
        say(title() + "\nOK: yakınlaştır  •  OK uzun bas / Menü: slayt  •  ◀ ▶: önceki/sonraki");
        new Thread(() -> {
            Bitmap bm = decode(f);
            runOnUiThread(() -> {
                if (t != token) { if (bm != null) bm.recycle(); return; }
                if (bm == null) { say("Resim açılamadı"); return; }
                z.set(bm);
            });
        }).start();
    }

    String title() {
        if (one != null) { String n = one.getLastPathSegment(); return n == null ? "Resim" : n; }
        return list.get(idx).getName() + "  •  " + (idx + 1) + "/" + list.size();
    }

    InputStream open(File f) throws Exception {
        return f != null ? new java.io.FileInputStream(f) : getContentResolver().openInputStream(one);
    }

    Bitmap decode(File f) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = open(f);
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int ss = 1;
            while (o.outWidth / ss > 3072 || o.outHeight / ss > 3072) ss *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = ss;
            in = open(f);
            Bitmap b = BitmapFactory.decodeStream(in, null, o);
            in.close();
            if (b == null) return null;
            int deg = 0;
            try {
                ExifInterface ex = null;
                if (f != null) ex = new ExifInterface(f.getPath());
                else if (Build.VERSION.SDK_INT >= 24) { InputStream i2 = open(null); ex = new ExifInterface(i2); }
                if (ex != null) {
                    int or = ex.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                    deg = or == 6 ? 90 : or == 3 ? 180 : or == 8 ? 270 : 0;
                }
            } catch (Exception e) { }
            if (deg != 0) {
                Matrix m = new Matrix(); m.postRotate(deg);
                Bitmap r = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (r != b) b.recycle();
                b = r;
            }
            return b;
        } catch (Throwable e) { return null; }
    }

    void say(String s) {
        info.setText(s); info.setVisibility(View.VISIBLE);
        h.removeCallbacks(hide); h.postDelayed(hide, 3500);
    }

    void go(int d) {
        if (one != null) return;
        idx = (idx + d + list.size()) % list.size();
        load();
    }

    void zoom() {
        zi = (zi + 1) % ZS.length;
        z.zoom = ZS[zi]; z.ox = z.oy = 0; z.invalidate();
        say("Yakınlaştırma: " + (int) (ZS[zi] * 100) + "%" + (zi > 0 ? "  (ok tuşları: kaydır)" : ""));
    }

    void toggleSlide() {
        if (one != null) return;
        slide = !slide;
        h.removeCallbacks(tick);
        if (slide) h.postDelayed(tick, 4000);
        say(slide ? "Slayt gösterisi başladı" : "Slayt gösterisi durdu");
    }

    @Override public boolean onKeyDown(int k, KeyEvent e) {
        float st = U.dp(this, 60) * (1 + Math.min(e.getRepeatCount(), 10) / 3f);
        switch (k) {
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:
                if (e.getRepeatCount() == 0) e.startTracking();
                return true;
            case KeyEvent.KEYCODE_MENU: case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: case KeyEvent.KEYCODE_MEDIA_PLAY: case KeyEvent.KEYCODE_MEDIA_PAUSE:
                toggleSlide(); return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (z.zoom > 1) z.pan(st, 0); else go(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (z.zoom > 1) z.pan(-st, 0); else go(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (z.zoom > 1) z.pan(0, st); else say(title());
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (z.zoom > 1) z.pan(0, -st); else say(title());
                return true;
            case KeyEvent.KEYCODE_PAGE_DOWN: case KeyEvent.KEYCODE_CHANNEL_DOWN: case KeyEvent.KEYCODE_MEDIA_NEXT:
                go(1); return true;
            case KeyEvent.KEYCODE_PAGE_UP: case KeyEvent.KEYCODE_CHANNEL_UP: case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                go(-1); return true;
        }
        return super.onKeyDown(k, e);
    }

    @Override public boolean onKeyLongPress(int k, KeyEvent e) {
        if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) { toggleSlide(); return true; }
        return super.onKeyLongPress(k, e);
    }

    @Override public boolean onKeyUp(int k, KeyEvent e) {
        if ((k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) && !e.isCanceled()) { zoom(); return true; }
        return super.onKeyUp(k, e);
    }

    @Override protected void onPause() {
        super.onPause();
        slide = false;
        h.removeCallbacks(tick);
    }

    class Z extends View {
        Bitmap b;
        float zoom = 1, ox, oy;
        Matrix m = new Matrix();
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);

        Z() { super(ImgActivity.this); }

        void set(Bitmap n) {
            Bitmap old = b;
            b = n; zoom = 1; ox = oy = 0;
            invalidate();
            if (old != null) old.recycle();
        }

        void pan(float dx, float dy) { ox += dx; oy += dy; invalidate(); }

        @Override protected void onDraw(Canvas c) {
            if (b == null) return;
            float W = getWidth(), H = getHeight();
            float s = Math.min(W / b.getWidth(), H / b.getHeight()) * zoom;
            float w = b.getWidth() * s, hh = b.getHeight() * s;
            float mx = Math.max(0, (w - W) / 2), my = Math.max(0, (hh - H) / 2);
            ox = Math.max(-mx, Math.min(mx, ox));
            oy = Math.max(-my, Math.min(my, oy));
            m.setScale(s, s);
            m.postTranslate((W - w) / 2 + ox, (H - hh) / 2 + oy);
            c.drawBitmap(b, m, p);
        }
    }
}
