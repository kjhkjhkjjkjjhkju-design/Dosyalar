package com.veysel.dosyalar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class Th {
    static final LruCache<String, Bitmap> C = new LruCache<String, Bitmap>(10 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };
    static final ThreadPoolExecutor EX = new ThreadPoolExecutor(2, 2, 3, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    static final Handler H = new Handler(Looper.getMainLooper());
    static { EX.allowCoreThreadTimeOut(true); }

    static void load(final ImageView iv, final TextView fb, final String path, final boolean vid, final int px) {
        final String key = path + "|" + px;
        Bitmap b = C.get(key);
        if (b != null) { iv.setImageBitmap(b); iv.setVisibility(View.VISIBLE); fb.setVisibility(View.GONE); return; }
        iv.setVisibility(View.GONE); fb.setVisibility(View.VISIBLE);
        EX.execute(() -> {
            if (!path.equals(iv.getTag())) return;
            final Bitmap r = vid ? vthumb(path, px) : ithumb(path, px);
            if (r == null) return;
            C.put(key, r);
            H.post(() -> {
                if (path.equals(iv.getTag())) { iv.setImageBitmap(r); iv.setVisibility(View.VISIBLE); fb.setVisibility(View.GONE); }
            });
        });
    }

    static Bitmap ithumb(String p, int px) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(p, o);
            int ss = 1, m = Math.min(o.outWidth, o.outHeight);
            while (m / (ss * 2) >= px) ss *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = ss;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(p, o);
        } catch (Throwable e) { return null; }
    }

    @SuppressWarnings("deprecation")
    static Bitmap vthumb(String p, int px) {
        try {
            Bitmap b = ThumbnailUtils.createVideoThumbnail(p, MediaStore.Video.Thumbnails.MINI_KIND);
            if (b == null) return null;
            if (b.getHeight() > px) {
                Bitmap s = Bitmap.createScaledBitmap(b, Math.max(1, b.getWidth() * px / b.getHeight()), px, true);
                if (s != b) b.recycle();
                b = s;
            }
            return b;
        } catch (Throwable e) { return null; }
    }
}
