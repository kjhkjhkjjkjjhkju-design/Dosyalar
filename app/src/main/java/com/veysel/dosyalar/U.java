package com.veysel.dosyalar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

final class U {
    static final int BG = 0xFF0E1420, CARD = 0xFF1A2333, ACC = 0xFF00B8D4, TXT = 0xFFEAF0F8, SUB = 0xFF8FA0B8;

    static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density + .5f); }

    static String ext(String n) { int i = n.lastIndexOf('.'); return i < 0 ? "" : n.substring(i + 1).toLowerCase(); }
    static int kind(String n) {
        switch (ext(n)) {
            case "jpg": case "jpeg": case "png": case "webp": case "bmp": case "gif": return 2;
            case "mp4": case "mkv": case "avi": case "mov": case "webm": case "3gp": case "ts": case "m4v": case "mpg": case "mpeg": case "wmv": case "flv": return 3;
            case "pdf": return 4;
            case "apk": return 5;
            case "mp3": case "wav": case "flac": case "m4a": case "ogg": case "aac": return 6;
            case "doc": case "docx": case "xls": case "xlsx": case "ppt": case "pptx": case "txt": case "odt": case "ods": case "epub": case "rtf": case "csv": return 7;
        }
        return 0;
    }
    static boolean isImg(String n) { return kind(n) == 2; }
    static boolean isVid(String n) { return kind(n) == 3; }
    static boolean isDoc(String n) { int k = kind(n); return k == 4 || k == 7; }

    static android.content.SharedPreferences sp(Context c) { return c.getSharedPreferences("s", 0); }
    static final SimpleDateFormat DF = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    static String size(long b) {
        if (b < 1024) return b + " B";
        double k = b / 1024.0;
        if (k < 1024) return String.format("%.1f KB", k);
        double m = k / 1024;
        if (m < 1024) return String.format("%.1f MB", m);
        return String.format("%.2f GB", m / 1024);
    }

    static String date(long t) { synchronized (DF) { return DF.format(new Date(t)); } }

    static String time(int ms) {
        int s = ms / 1000;
        return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format("%02d:%02d", s / 60, s % 60);
    }

    static GradientDrawable rect(int color, int radius, int strokeW, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(radius);
        if (strokeW > 0) g.setStroke(strokeW, stroke);
        return g;
    }

    static StateListDrawable focusBg(Context c, int normal, int radius) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, rect(0xFF24344F, radius, dp(c, 3), ACC));
        s.addState(new int[]{}, rect(normal, radius, 0, 0));
        return s;
    }

    static TextView btn(Context c, String t, View.OnClickListener l) {
        TextView v = new TextView(c);
        v.setText(t); v.setTextColor(TXT); v.setTextSize(15);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
        v.setFocusable(true); v.setClickable(true);
        v.setBackground(focusBg(c, CARD, dp(c, 10)));
        v.setOnClickListener(l);
        return v;
    }

    static void del(File f) {
        if (f.isDirectory()) { File[] a = f.listFiles(); if (a != null) for (File c : a) del(c); }
        f.delete();
    }

    static void copy(File s, File d) throws IOException {
        if (s.isDirectory()) {
            d.mkdirs();
            File[] a = s.listFiles();
            if (a != null) for (File c : a) copy(c, new File(d, c.getName()));
        } else {
            try (InputStream i = new FileInputStream(s); OutputStream o = new FileOutputStream(d)) {
                byte[] b = new byte[65536]; int n;
                while ((n = i.read(b)) > 0) o.write(b, 0, n);
            }
        }
    }

    static long len(File f) {
        if (!f.isDirectory()) return f.length();
        long t = 0; File[] a = f.listFiles();
        if (a != null) for (File c : a) t += len(c);
        return t;
    }

    static File uniq(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return f;
        int i = name.lastIndexOf('.');
        String b = i > 0 ? name.substring(0, i) : name, e = i > 0 ? name.substring(i) : "";
        for (int n = 1; ; n++) { f = new File(dir, b + " (" + n + ")" + e); if (!f.exists()) return f; }
    }
}
