package com.veysel.dosyalar;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        perm();
        build();
    }

    void perm() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())));
                } catch (Exception e) { }
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    TextView tv(String t, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return v;
    }

    void build() {
        int p = U.dp(this, 40);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{0xFF16233A, 0xFF0A0F1A}));
        root.setPadding(p, p, p, U.dp(this, 20));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout tt = new LinearLayout(this);
        tt.setOrientation(LinearLayout.VERTICAL);
        tt.addView(tv("Dosyalar", 40, U.TXT, true));
        tt.addView(tv("TV Dosya Yöneticisi", 16, U.ACC, false));
        top.addView(tt, new LinearLayout.LayoutParams(0, -2, 1f));
        top.addView(U.btn(this, "⚙ Ayarlar", v -> settings()));
        root.addView(top);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(row, rp);

        File ext = Environment.getExternalStorageDirectory();
        String free = U.size(ext.getFreeSpace()) + " boş / " + U.size(ext.getTotalSpace());
        add(row, "📱", "Dahili depolama", free, 0, ext.getPath());
        add(row, "⬇️", "İndirilenler", "Download", 0, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
        add(row, "🖼️", "Fotoğraflar", "Tüm resimler", 1, null);
        add(row, "🎬", "Videolar", "Tüm videolar", 2, null);
        add(row, "📄", "Belgeler", "PDF, Word, Excel…", 3, null);
        File[] vols = new File("/storage").listFiles();
        if (vols != null) for (File v : vols) {
            String n = v.getName();
            if (n.equals("emulated") || n.equals("self") || !v.canRead() || v.listFiles() == null) continue;
            add(row, "💾", "USB / SD", n, 0, v.getPath());
            break;
        }

        TextView f = tv("Yapımcı: Veysel Alemdar", 16, U.SUB, false);
        f.setGravity(Gravity.CENTER);
        root.addView(f, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    void settings() {
        final android.content.SharedPreferences sp = U.sp(this);
        final boolean[] on = {sp.getBoolean("thumb", false), sp.getBoolean("grid", false)};
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Ayarlar")
            .setMultiChoiceItems(new String[]{"Küçük resim önizleme (fotoğraf / video)", "Izgara görünümü"}, on,
                (d, i, c) -> sp.edit().putBoolean(i == 0 ? "thumb" : "grid", c).apply())
            .setPositiveButton("Tamam", null).show();
    }

    void add(LinearLayout row, String icon, final String title, String sub, final int cat, final String path) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setFocusable(true); c.setClickable(true);
        c.setBackground(U.focusBg(this, U.CARD, U.dp(this, 18)));
        c.addView(tv(icon, 52, U.TXT, false));
        TextView t = tv(title, 19, U.TXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, U.dp(this, 10), 0, U.dp(this, 2));
        c.addView(t);
        TextView s = tv(sub, 12, U.SUB, false);
        s.setGravity(Gravity.CENTER);
        c.addView(s);
        c.setOnFocusChangeListener((v, has) -> v.animate().scaleX(has ? 1.07f : 1f).scaleY(has ? 1.07f : 1f).setDuration(110).start());
        c.setOnClickListener(v -> {
            Intent i = new Intent(this, BrowseActivity.class);
            i.putExtra("cat", cat).putExtra("title", title);
            if (path != null) i.putExtra("path", path);
            startActivity(i);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, U.dp(this, 190), 1f);
        int m = U.dp(this, 8);
        lp.setMargins(m, m, m, m);
        row.addView(c, lp);
        if (row.getChildCount() == 1) c.requestFocus();
    }
}
