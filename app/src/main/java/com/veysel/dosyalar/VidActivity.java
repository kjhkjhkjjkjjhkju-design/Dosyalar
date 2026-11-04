package com.veysel.dosyalar;

import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;

public class VidActivity extends Activity implements SurfaceHolder.Callback {
    static ArrayList<File> list;
    static int idx;

    MediaPlayer mp;
    SurfaceView sv;
    FrameLayout root;
    LinearLayout bar;
    TextView title, time;
    ProgressBar pb;
    Handler h = new Handler();
    float speed = 1f;
    int resume;
    boolean ready, surface;
    final Runnable hide = () -> bar.setVisibility(View.GONE);
    final Runnable tick = new Runnable() {
        public void run() {
            upd();
            if (bar.getVisibility() == View.VISIBLE) h.postDelayed(this, 500);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (list == null || list.isEmpty()) { finish(); return; }
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackgroundColor(0xB3000000);
        int p = U.dp(this, 24);
        bar.setPadding(p, U.dp(this, 12), p, U.dp(this, 14));
        title = new TextView(this); title.setTextColor(Color.WHITE); title.setTextSize(18); title.setSingleLine(true);
        pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setProgressTintList(android.content.res.ColorStateList.valueOf(U.ACC));
        time = new TextView(this); time.setTextColor(0xFFCCD6E4); time.setTextSize(14);
        bar.addView(title);
        bar.addView(pb, new LinearLayout.LayoutParams(-1, U.dp(this, 8)));
        bar.addView(time);
        root.addView(bar, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        setContentView(root);
    }

    @Override public void surfaceCreated(SurfaceHolder s) { surface = true; start(); }
    @Override public void surfaceChanged(SurfaceHolder s, int f, int w, int hh) { }
    @Override public void surfaceDestroyed(SurfaceHolder s) {
        surface = false;
        if (mp != null) { resume = mp.getCurrentPosition(); mp.release(); mp = null; ready = false; }
    }

    void start() {
        if (!surface || mp != null) return;
        File f = list.get(idx);
        title.setText(f.getName() + "   (" + (idx + 1) + "/" + list.size() + ")");
        try {
            mp = new MediaPlayer();
            mp.setDataSource(f.getPath());
            mp.setDisplay(sv.getHolder());
            mp.setOnVideoSizeChangedListener((m, w, hh) -> fit(w, hh));
            mp.setOnPreparedListener(m -> {
                ready = true;
                pb.setMax(m.getDuration());
                if (resume > 0) { m.seekTo(resume); resume = 0; }
                m.start();
                setSpeed(speed);
                show();
            });
            mp.setOnCompletionListener(m -> {
                if (idx + 1 < list.size()) { idx++; release(); start(); } else finish();
            });
            mp.setOnErrorListener((m, w, e) -> { Toast.makeText(this, "Video oynatılamıyor", Toast.LENGTH_LONG).show(); finish(); return true; });
            mp.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(this, "Video açılamadı", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    void release() { if (mp != null) { mp.release(); mp = null; ready = false; } }

    void fit(int vw, int vh) {
        int W = root.getWidth(), H = root.getHeight();
        if (vw <= 0 || vh <= 0 || W <= 0) return;
        float s = Math.min((float) W / vw, (float) H / vh);
        sv.setLayoutParams(new FrameLayout.LayoutParams((int) (vw * s), (int) (vh * s), Gravity.CENTER));
    }

    void setSpeed(float s) {
        if (mp == null || !ready || Build.VERSION.SDK_INT < 23) return;
        try {
            boolean was = mp.isPlaying();
            mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(s));
            if (!was) mp.pause();
            speed = s;
        } catch (Exception e) { }
    }

    void upd() {
        if (mp == null || !ready) return;
        int d = mp.getDuration(), c = mp.getCurrentPosition();
        pb.setProgress(c);
        time.setText((mp.isPlaying() ? "▶ " : "⏸ ") + U.time(c) + " / " + U.time(d) + "     Hız: " + speed + "x");
    }

    void show() {
        bar.setVisibility(View.VISIBLE);
        upd();
        h.removeCallbacks(tick); h.removeCallbacks(hide);
        h.post(tick); h.postDelayed(hide, 4000);
    }

    void seek(int ms) {
        if (mp == null || !ready) return;
        mp.seekTo(Math.max(0, Math.min(mp.getDuration(), mp.getCurrentPosition() + ms)));
        show();
    }

    @Override public boolean onKeyDown(int k, KeyEvent e) {
        if (mp == null || !ready) return super.onKeyDown(k, e);
        switch (k) {
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: case KeyEvent.KEYCODE_SPACE:
                if (e.getRepeatCount() == 0) { if (mp.isPlaying()) mp.pause(); else mp.start(); show(); }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY: mp.start(); show(); return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE: mp.pause(); show(); return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_MEDIA_REWIND:
                seek(e.getRepeatCount() > 4 ? -30000 : -10000); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seek(e.getRepeatCount() > 4 ? 30000 : 10000); return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (e.getRepeatCount() == 0) { setSpeed(Math.min(2f, speed + 0.25f)); show(); }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (e.getRepeatCount() == 0) { setSpeed(Math.max(0.5f, speed - 0.25f)); show(); }
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT: case KeyEvent.KEYCODE_CHANNEL_UP:
                if (idx + 1 < list.size()) { idx++; release(); start(); }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: case KeyEvent.KEYCODE_CHANNEL_DOWN:
                if (idx > 0) { idx--; release(); start(); }
                return true;
        }
        return super.onKeyDown(k, e);
    }

    @Override protected void onPause() {
        super.onPause();
        if (mp != null && ready && mp.isPlaying()) mp.pause();
        h.removeCallbacks(tick);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        h.removeCallbacksAndMessages(null);
        release();
    }
}
