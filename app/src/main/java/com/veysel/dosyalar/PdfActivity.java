package com.veysel.dosyalar;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfActivity extends Activity {
    File tmp;
    PdfRenderer r;
    ParcelFileDescriptor fd;
    V v;
    TextView info;
    Handler h = new Handler();
    int page, pages, level, token;
    int atEdge; // 1: yeni sayfa üstten, -1: alttan
    final String[] LN = {"Sığdır", "Genişliğe sığdır", "%150", "%200", "%300"};
    final float[] LM = {0, 1f, 1.5f, 2f, 3f};
    final Object lock = new Object();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        String path = getIntent().getStringExtra("p");
        Uri d = getIntent().getData();
        try {
            if (d == null) fd = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
            else if ("file".equals(d.getScheme())) fd = ParcelFileDescriptor.open(new File(d.getPath()), ParcelFileDescriptor.MODE_READ_ONLY);
            else {
                try {
                    fd = getContentResolver().openFileDescriptor(d, "r");
                    r = new PdfRenderer(fd);
                } catch (Exception e) {
                    if (fd != null) fd.close();
                    tmp = new File(getCacheDir(), "t.pdf");
                    try (InputStream in = getContentResolver().openInputStream(d); OutputStream out = new java.io.FileOutputStream(tmp)) {
                        byte[] bf = new byte[65536]; int n;
                        while ((n = in.read(bf)) > 0) out.write(bf, 0, n);
                    }
                    fd = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY);
                    r = null;
                }
            }
            if (r == null) r = new PdfRenderer(fd);
            pages = r.getPageCount();
        } catch (Exception e) {
            Toast.makeText(this, "PDF açılamadı (şifreli veya bozuk olabilir)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF2B2F36);
        v = new V();
        root.addView(v, new FrameLayout.LayoutParams(-1, -1));
        info = new TextView(this);
        info.setTextColor(Color.WHITE); info.setTextSize(14);
        info.setBackgroundColor(0xCC000000);
        info.setPadding(U.dp(this, 14), U.dp(this, 6), U.dp(this, 14), U.dp(this, 6));
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        ip.setMargins(0, U.dp(this, 12), U.dp(this, 12), 0);
        root.addView(info, ip);
        setContentView(root);
        root.post(this::render);
    }

    void upd() { info.setText((page + 1) + " / " + pages + "  •  " + LN[level] + "  •  OK uzun bas: seçenekler"); }

    void render() {
        final int t = ++token, W = v.getWidth(), H = v.getHeight(), pg = page, lv = level;
        if (W <= 0) return;
        upd();
        new Thread(() -> {
            Bitmap bm = null; float dw = 0, dh = 0;
            synchronized (lock) {
                if (t != token || r == null) return;
                try {
                    PdfRenderer.Page p = r.openPage(pg);
                    float pw = p.getWidth(), ph = p.getHeight();
                    float s = lv == 0 ? Math.min(W / pw, H / ph) : W / pw * LM[lv];
                    dw = pw * s; dh = ph * s;
                    float k = 1f;
                    float px = dw * dh;
                    if (px > 10e6f) k = (float) Math.sqrt(10e6f / px);
                    bm = Bitmap.createBitmap(Math.max(1, (int) (dw * k)), Math.max(1, (int) (dh * k)), Bitmap.Config.ARGB_8888);
                    bm.eraseColor(Color.WHITE);
                    p.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    p.close();
                } catch (Throwable e) { bm = null; }
            }
            final Bitmap fb = bm; final float fw = dw, fh = dh;
            runOnUiThread(() -> { if (t == token) v.set(fb, fw, fh); });
        }).start();
    }

    void goPage(int p, int edge) {
        if (p < 0 || p >= pages) return;
        page = p; atEdge = edge;
        render();
    }

    void setLevel(int l) {
        level = Math.max(0, Math.min(LM.length - 1, l));
        atEdge = level > 0 ? 1 : 0;
        render();
    }

    @Override public boolean onKeyDown(int k, KeyEvent e) {
        float st = U.dp(this, 70) * (1 + Math.min(e.getRepeatCount(), 10) / 3f);
        switch (k) {
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:
                if (e.getRepeatCount() == 0) e.startTracking();
                return true;
            case KeyEvent.KEYCODE_MENU: menu(); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (v.canDown()) v.pan(0, -st); else goPage(page + 1, 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (v.canUp()) v.pan(0, st); else goPage(page - 1, -1);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (v.canRight()) v.pan(-st, 0); else goPage(page + 1, 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (v.canLeft()) v.pan(st, 0); else goPage(page - 1, -1);
                return true;
            case KeyEvent.KEYCODE_PAGE_DOWN: case KeyEvent.KEYCODE_CHANNEL_DOWN: goPage(page + 1, 1); return true;
            case KeyEvent.KEYCODE_PAGE_UP: case KeyEvent.KEYCODE_CHANNEL_UP: goPage(page - 1, 1); return true;
        }
        return super.onKeyDown(k, e);
    }

    @Override public boolean onKeyLongPress(int k, KeyEvent e) {
        if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) { menu(); return true; }
        return super.onKeyLongPress(k, e);
    }

    @Override public boolean onKeyUp(int k, KeyEvent e) {
        if ((k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) && !e.isCanceled()) {
            setLevel(level + 1 >= LM.length ? 0 : level + 1);
            return true;
        }
        return super.onKeyUp(k, e);
    }

    void menu() {
        String[] ops = {"Sayfaya git (Bul)", "Sığdır", "Genişliğe sığdır", "Yakınlaştır", "Uzaklaştır", "İlk sayfa", "Son sayfa"};
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setItems(ops, (d, i) -> {
            switch (i) {
                case 0: gotoDlg(); break;
                case 1: setLevel(0); break;
                case 2: setLevel(1); break;
                case 3: setLevel(level + 1); break;
                case 4: setLevel(level - 1); break;
                case 5: goPage(0, 1); break;
                default: goPage(pages - 1, 1);
            }
        }).show();
    }

    void gotoDlg() {
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER); et.setHint("1 - " + pages);
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Sayfaya git").setView(et)
            .setNegativeButton("İptal", null).setPositiveButton("Git", (d, w) -> {
                try { goPage(Integer.parseInt(et.getText().toString().trim()) - 1, 1); } catch (Exception e) { }
            }).show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        token++;
        synchronized (lock) {
            try { if (r != null) r.close(); if (fd != null) fd.close(); } catch (Exception e) { }
            r = null;
        }
        if (tmp != null) tmp.delete();
    }

    class V extends View {
        Bitmap b;
        float dw, dh, ox, oy;
        RectF dst = new RectF();
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);

        V() { super(PdfActivity.this); }

        void set(Bitmap n, float w, float hh) {
            Bitmap old = b;
            b = n; dw = w; dh = hh; ox = 0;
            oy = atEdge == 1 ? 1e7f : atEdge == -1 ? -1e7f : 0;
            atEdge = 0;
            invalidate();
            if (old != null) old.recycle();
            if (n == null) Toast.makeText(PdfActivity.this, "Sayfa çizilemedi", Toast.LENGTH_SHORT).show();
        }

        float mx() { return Math.max(0, (dw - getWidth()) / 2); }
        float my() { return Math.max(0, (dh - getHeight()) / 2); }
        boolean canDown() { return oy > -my() + 1; }
        boolean canUp() { return oy < my() - 1; }
        boolean canRight() { return ox > -mx() + 1; }
        boolean canLeft() { return ox < mx() - 1; }
        void pan(float dx, float dy) { ox += dx; oy += dy; invalidate(); }

        @Override protected void onDraw(Canvas c) {
            if (b == null) return;
            ox = Math.max(-mx(), Math.min(mx(), ox));
            oy = Math.max(-my(), Math.min(my(), oy));
            float l = (getWidth() - dw) / 2 + ox, t = (getHeight() - dh) / 2 + oy;
            dst.set(l, t, l + dw, t + dh);
            c.drawBitmap(b, null, dst, p);
        }
    }
}
