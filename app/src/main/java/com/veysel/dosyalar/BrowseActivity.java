package com.veysel.dosyalar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class BrowseActivity extends Activity {
    static File clip;
    static boolean clipMove;
    static final SparseArray<ArrayList<E>> CC = new SparseArray<>();
    static final String[] IC = {"📄", "📁", "🖼️", "🎬", "📕", "📦", "🎵", "📄"};

    // type: 0 diğer, 1 klasör, 2 resim, 3 video, 4 pdf, 5 apk, 6 ses, 7 belge
    static final class E {
        final File f; final String name; final boolean dir; final long size, mod; final int type; String sub;
        E(File f, boolean isFile) {
            this.f = f; name = f.getName();
            dir = !isFile && f.isDirectory();
            size = dir ? 0 : f.length(); mod = f.lastModified();
            type = dir ? 1 : U.kind(name);
        }
    }

    static void sort(ArrayList<E> l, final int mode) {
        Collections.sort(l, new Comparator<E>() {
            public int compare(E a, E b) {
                if (a.dir != b.dir) return a.dir ? -1 : 1;
                if (mode == 1) return Long.compare(b.mod, a.mod);
                if (mode == 2) return Long.compare(b.size, a.size);
                return a.name.compareToIgnoreCase(b.name);
            }
        });
    }

    File root, cur;
    int cat, sort, tok;
    boolean grid, thumbs, scanning;
    ArrayList<E> items = new ArrayList<>();
    ListView ll; GridView gv; AbsListView lv;
    Ad la, ga;
    TextView path, empty, count, gbtn;
    View paste, newDir;
    SharedPreferences sp;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = U.sp(this);
        grid = sp.getBoolean("grid", false);
        thumbs = sp.getBoolean("thumb", false);
        cat = getIntent().getIntExtra("cat", 0);
        String title = getIntent().getStringExtra("title");
        if (cat == 0) root = cur = new File(getIntent().getStringExtra("path"));
        sort = cat == 0 ? 0 : 1;

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(U.BG);
        int p = U.dp(this, 32);
        page.setPadding(p, U.dp(this, 20), p, U.dp(this, 12));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        path = new TextView(this);
        path.setTextColor(U.TXT); path.setTextSize(18);
        path.setSingleLine(true); path.setEllipsize(TextUtils.TruncateAt.START);
        path.setText(cat == 0 ? cur.getPath() : title);
        head.addView(path, new LinearLayout.LayoutParams(0, -2, 1f));
        paste = U.btn(this, "Yapıştır", v -> paste());
        newDir = U.btn(this, "Yeni klasör", v -> newFolder());
        gbtn = U.btn(this, "Izgara", v -> toggleGrid());
        View srt = U.btn(this, "Sırala", v -> sortDialog());
        int m = U.dp(this, 8);
        for (View x : new View[]{paste, newDir, gbtn, srt}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(m, 0, 0, 0);
            head.addView(x, lp);
        }
        paste.setVisibility(View.GONE);
        if (cat != 0) newDir.setVisibility(View.GONE);
        page.addView(head);

        la = new Ad(false); ga = new Ad(true);
        FrameLayout body = new FrameLayout(this);
        ll = new ListView(this);
        ll.setDivider(null);
        gv = new GridView(this);
        gv.setNumColumns(GridView.AUTO_FIT);
        gv.setColumnWidth(U.dp(this, 150));
        gv.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        gv.setHorizontalSpacing(U.dp(this, 10));
        gv.setVerticalSpacing(U.dp(this, 10));
        gv.setClipToPadding(false);
        AdapterView.OnItemClickListener cl = (a, v, i, id) -> open(items.get(i));
        AdapterView.OnItemLongClickListener lc = (a, v, i, id) -> { actions(items.get(i)); return true; };
        for (AbsListView x : new AbsListView[]{ll, gv}) {
            x.setOnItemClickListener(cl);
            x.setOnItemLongClickListener(lc);
            x.setSelector(U.rect(0x4400B8D4, U.dp(this, 10), U.dp(this, 2), U.ACC));
            body.addView(x, new FrameLayout.LayoutParams(-1, -1));
        }
        empty = new TextView(this);
        empty.setTextColor(U.SUB); empty.setTextSize(20); empty.setGravity(Gravity.CENTER);
        body.addView(empty, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, 0, 1f);
        bp.setMargins(0, U.dp(this, 10), 0, U.dp(this, 6));
        page.addView(body, bp);

        count = new TextView(this);
        count.setTextColor(U.SUB); count.setTextSize(13);
        page.addView(count);
        setContentView(page);
        mode();
        load(null);
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    void mode() {
        ll.setAdapter(grid ? null : la);
        gv.setAdapter(grid ? ga : null);
        ll.setVisibility(grid ? View.GONE : View.VISIBLE);
        gv.setVisibility(grid ? View.VISIBLE : View.GONE);
        lv = grid ? gv : ll;
        gbtn.setText(grid ? "Liste" : "Izgara");
    }

    void toggleGrid() {
        int sel = lv.getSelectedItemPosition();
        grid = !grid;
        sp.edit().putBoolean("grid", grid).apply();
        mode();
        lv.setSelection(sel < 0 ? 0 : sel);
        lv.requestFocus();
    }

    void done(File focus) {
        la.notifyDataSetChanged(); ga.notifyDataSetChanged();
        int sel = 0;
        if (focus != null) for (int i = 0; i < items.size(); i++) if (items.get(i).f.equals(focus)) { sel = i; break; }
        lv.setSelection(sel);
        lv.requestFocus();
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(scanning ? "Taranıyor…" : "Dosya yok");
        count.setText(items.size() + " öğe  •  Uzun bas / Menü tuşu: işlemler");
        paste.setVisibility(clip != null && cat == 0 ? View.VISIBLE : View.GONE);
    }

    void refresh(File focus) { CC.clear(); load(focus); }

    void load(final File focus) {
        final int t = ++tok;
        if (cat == 0) {
            final File c = cur;
            path.setText(c.getPath());
            new Thread(() -> {
                File[] a = c.listFiles();
                ArrayList<E> r = new ArrayList<>();
                if (a != null) for (File f : a) if (f.getName().charAt(0) != '.') r.add(new E(f, false));
                sort(r, sort);
                runOnUiThread(() -> { if (t == tok) { items = r; done(focus); } });
            }).start();
            return;
        }
        ArrayList<E> c = CC.get(cat);
        items = c == null ? new ArrayList<E>() : new ArrayList<>(c);
        scanning = true;
        done(null);
        new Thread(() -> {
            ArrayList<E> r = new ArrayList<>();
            scan(Environment.getExternalStorageDirectory(), 0, r);
            sort(r, sort);
            CC.put(cat, r);
            runOnUiThread(() -> {
                scanning = false;
                if (t != tok) return;
                if (same(items, r)) { empty.setText("Dosya yok"); return; }
                int i = lv.getSelectedItemPosition();
                File f = i >= 0 && i < items.size() ? items.get(i).f : null;
                items = new ArrayList<>(r);
                done(f);
            });
        }).start();
    }

    static boolean same(ArrayList<E> a, ArrayList<E> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (a.get(i).mod != b.get(i).mod || !a.get(i).f.equals(b.get(i).f)) return false;
        return true;
    }

    void scan(File d, int depth, ArrayList<E> out) {
        File[] a = d.listFiles();
        if (a == null) return;
        for (File f : a) {
            String n = f.getName();
            if (n.charAt(0) == '.' || (depth == 0 && n.equals("Android"))) continue;
            int k = U.kind(n);
            if (k != 0) {
                if (cat == 1 ? k == 2 : cat == 2 ? k == 3 : (k == 4 || k == 7)) out.add(new E(f, true));
            } else if (depth < 12 && f.isDirectory()) scan(f, depth + 1, out);
        }
    }

    @Override public void onBackPressed() {
        if (cat == 0 && !cur.equals(root) && cur.getParentFile() != null) {
            File prev = cur;
            cur = cur.getParentFile();
            load(prev);
        } else super.onBackPressed();
    }

    @Override public boolean onKeyDown(int k, KeyEvent e) {
        if (k == KeyEvent.KEYCODE_MENU) {
            int i = lv.getSelectedItemPosition();
            if (i >= 0 && i < items.size()) actions(items.get(i));
            return true;
        }
        return super.onKeyDown(k, e);
    }

    void open(E e) {
        File f = e.f;
        if (e.dir) { cur = f; load(null); return; }
        if (e.type == 2 || e.type == 3) {
            ArrayList<File> l = new ArrayList<>(); int k = 0;
            for (E x : items) if (x.type == e.type) { if (x == e) k = l.size(); l.add(x.f); }
            if (e.type == 2) { ImgActivity.list = l; ImgActivity.idx = k; startActivity(new Intent(this, ImgActivity.class)); }
            else { VidActivity.list = l; VidActivity.idx = k; startActivity(new Intent(this, VidActivity.class)); }
        } else if (e.type == 4) {
            startActivity(new Intent(this, PdfActivity.class).putExtra("p", f.getPath()));
        } else openExt(f, e.type == 5);
    }

    void openExt(File f, boolean apk) {
        if (apk && Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            toast("Önce bu uygulamaya \"Bilinmeyen uygulamaları yükle\" izni ver, sonra tekrar aç");
            try { startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()))); } catch (Exception e) { }
            return;
        }
        Intent in = new Intent(Intent.ACTION_VIEW);
        in.setDataAndType(FP.uri(f), apk ? "application/vnd.android.package-archive" : FP.mime(f));
        in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(in); } catch (Exception e) { toast(apk ? "Yükleyici bulunamadı" : "Bu dosyayı açacak uygulama yok"); }
    }

    AlertDialog.Builder dlg() { return new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK); }

    void actions(final E e) {
        final File f = e.f;
        String[] ops = {"Aç", "Yeniden adlandır", "Kopyala", "Taşı", "Sil", "Paylaş", "Bilgi"};
        dlg().setTitle(e.name).setItems(ops, (d, i) -> {
            switch (i) {
                case 0: open(e); break;
                case 1: rename(f); break;
                case 2: clip = f; clipMove = false; toast("Kopyalandı. Hedef klasörde \"Yapıştır\"a bas"); paste.setVisibility(cat == 0 ? View.VISIBLE : View.GONE); break;
                case 3: clip = f; clipMove = true; toast("Taşınacak. Hedef klasörde \"Yapıştır\"a bas"); paste.setVisibility(cat == 0 ? View.VISIBLE : View.GONE); break;
                case 4: delete(f); break;
                case 5: share(f); break;
                default: info(f);
            }
        }).show();
    }

    void rename(final File f) {
        final EditText et = new EditText(this);
        et.setText(f.getName()); et.setSingleLine(true);
        dlg().setTitle("Yeniden adlandır").setView(et).setNegativeButton("İptal", null)
            .setPositiveButton("Tamam", (d, w) -> {
                String n = et.getText().toString().trim();
                File t = new File(f.getParentFile(), n);
                if (n.isEmpty() || n.contains("/")) toast("Geçersiz ad");
                else if (t.exists()) toast("Bu ad zaten var");
                else if (f.renameTo(t)) refresh(t);
                else toast("Yeniden adlandırılamadı");
            }).show();
    }

    void newFolder() {
        final EditText et = new EditText(this);
        et.setText("Yeni klasör"); et.setSingleLine(true);
        dlg().setTitle("Yeni klasör").setView(et).setNegativeButton("İptal", null)
            .setPositiveButton("Oluştur", (d, w) -> {
                File t = new File(cur, et.getText().toString().trim());
                if (t.exists() || !t.mkdirs()) toast("Oluşturulamadı"); else refresh(t);
            }).show();
    }

    void delete(final File f) {
        dlg().setTitle("Silinsin mi?").setMessage(f.getName()).setNegativeButton("İptal", null)
            .setPositiveButton("Sil", (d, w) -> new Thread(() -> {
                U.del(f);
                runOnUiThread(() -> {
                    if (f.equals(clip)) clip = null;
                    refresh(null);
                    toast("Silindi");
                });
            }).start()).show();
    }

    void paste() {
        if (clip == null || cat != 0) return;
        final File src = clip, dest = U.uniq(cur, src.getName());
        final boolean mv = clipMove;
        if (src.isDirectory() && (cur.getPath() + "/").startsWith(src.getPath() + "/")) { toast("Klasör kendi içine yapıştırılamaz"); return; }
        toast("İşleniyor…");
        new Thread(() -> {
            boolean ok = false;
            try {
                if (mv) { if (!src.renameTo(dest)) { U.copy(src, dest); U.del(src); } }
                else U.copy(src, dest);
                ok = true;
            } catch (Exception e) { }
            final boolean r = ok;
            runOnUiThread(() -> {
                toast(r ? "Tamamlandı" : "İşlem başarısız");
                if (mv && r) clip = null;
                refresh(dest);
            });
        }).start();
    }

    void share(File f) {
        if (f.isDirectory()) { toast("Klasör paylaşılamaz"); return; }
        Intent in = new Intent(Intent.ACTION_SEND);
        in.setType(FP.mime(f)).putExtra(Intent.EXTRA_STREAM, FP.uri(f)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(Intent.createChooser(in, "Paylaş")); } catch (Exception e) { toast("Paylaşılamadı"); }
    }

    void info(final File f) {
        final String base = "Konum: " + f.getParent() + "\nTür: " + (f.isDirectory() ? "Klasör" : f.getName().contains(".") ? U.ext(f.getName()).toUpperCase() : "Dosya")
            + "\nDeğiştirilme: " + U.date(f.lastModified()) + "\nOkuma/Yazma: " + (f.canRead() ? "E" : "H") + "/" + (f.canWrite() ? "E" : "H");
        final AlertDialog d = dlg().setTitle(f.getName()).setMessage(base + "\nBoyut: hesaplanıyor…").setPositiveButton("Tamam", null).show();
        new Thread(() -> {
            long s = U.len(f);
            String extra = "";
            if (f.isDirectory()) { String[] a = f.list(); extra = "\nÖğe sayısı: " + (a == null ? 0 : a.length); }
            final String msg = base + "\nBoyut: " + U.size(s) + extra;
            runOnUiThread(() -> d.setMessage(msg));
        }).start();
    }

    void sortDialog() {
        dlg().setTitle("Sırala").setItems(new String[]{"Ada göre", "Tarihe göre (yeni)", "Boyuta göre"}, (d, i) -> {
            sort = i;
            int p = lv.getSelectedItemPosition();
            File f = p >= 0 && p < items.size() ? items.get(p).f : null;
            sort(items, sort);
            done(f);
        }).show();
    }

    String sub(E e) {
        if (e.sub == null) {
            File pf = e.f.getParentFile();
            e.sub = (cat != 0 && pf != null ? pf.getName() + "  •  " : "") + (e.dir ? "" : U.size(e.size) + "  •  ") + U.date(e.mod);
        }
        return e.sub;
    }

    static final class H { TextView ic, nm, sb; ImageView th; }

    class Ad extends BaseAdapter {
        final boolean g;
        Ad(boolean g) { this.g = g; }
        public int getCount() { return items.size(); }
        public Object getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int i, View v, ViewGroup parent) {
            Context c = BrowseActivity.this;
            H h;
            if (v == null) { h = new H(); v = g ? cell(c, h) : row(c, h); v.setTag(h); }
            else h = (H) v.getTag();
            E e = items.get(i);
            h.ic.setText(IC[e.type]);
            h.nm.setText(e.name);
            if (!g) h.sb.setText(sub(e));
            if (thumbs && (e.type == 2 || e.type == 3)) {
                String k = e.f.getPath();
                h.th.setTag(k);
                Th.load(h.th, h.ic, k, e.type == 3, U.dp(c, g ? 150 : 56));
            } else {
                h.th.setTag(null);
                h.th.setVisibility(View.GONE);
                h.ic.setVisibility(View.VISIBLE);
            }
            return v;
        }

        FrameLayout box(Context c, H h, int ics) {
            FrameLayout b = new FrameLayout(c);
            h.ic = new TextView(c); h.ic.setTextSize(ics); h.ic.setGravity(Gravity.CENTER);
            h.th = new ImageView(c); h.th.setScaleType(ImageView.ScaleType.CENTER_CROP);
            b.addView(h.th, new FrameLayout.LayoutParams(-1, -1));
            b.addView(h.ic, new FrameLayout.LayoutParams(-1, -1));
            return b;
        }

        View row(Context c, H h) {
            LinearLayout r = new LinearLayout(c);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(U.dp(c, 14), U.dp(c, 8), U.dp(c, 14), U.dp(c, 8));
            r.addView(box(c, h, 26), new LinearLayout.LayoutParams(U.dp(c, 56), U.dp(c, 42)));
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(U.dp(c, 12), 0, 0, 0);
            h.nm = new TextView(c); h.nm.setTextSize(18); h.nm.setTextColor(U.TXT); h.nm.setSingleLine(true); h.nm.setEllipsize(TextUtils.TruncateAt.END);
            h.sb = new TextView(c); h.sb.setTextSize(13); h.sb.setTextColor(U.SUB); h.sb.setSingleLine(true);
            col.addView(h.nm); col.addView(h.sb);
            r.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
            return r;
        }

        View cell(Context c, H h) {
            LinearLayout r = new LinearLayout(c);
            r.setOrientation(LinearLayout.VERTICAL);
            r.setPadding(U.dp(c, 6), U.dp(c, 6), U.dp(c, 6), U.dp(c, 6));
            r.setBackground(U.rect(U.CARD, U.dp(c, 12), 0, 0));
            r.setLayoutParams(new AbsListView.LayoutParams(-1, U.dp(c, 148)));
            r.addView(box(c, h, 42), new LinearLayout.LayoutParams(-1, U.dp(c, 96)));
            h.nm = new TextView(c); h.nm.setTextSize(13); h.nm.setTextColor(U.TXT); h.nm.setMaxLines(2); h.nm.setEllipsize(TextUtils.TruncateAt.END); h.nm.setGravity(Gravity.CENTER);
            h.nm.setPadding(0, U.dp(c, 4), 0, 0);
            r.addView(h.nm, new LinearLayout.LayoutParams(-1, -2));
            return r;
        }
    }
}
