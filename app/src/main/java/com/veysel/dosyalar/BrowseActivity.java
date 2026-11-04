package com.veysel.dosyalar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
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

    File root, cur;
    int cat, sort;
    ArrayList<File> items = new ArrayList<>();
    ListView lv;
    TextView path, empty, count;
    View paste, newDir;
    Ad ad = new Ad();
    boolean scanning;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
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
        View srt = U.btn(this, "Sırala", v -> sortDialog());
        int m = U.dp(this, 8);
        for (View v : new View[]{paste, newDir, srt}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(m, 0, 0, 0);
            head.addView(v, lp);
        }
        paste.setVisibility(View.GONE);
        if (cat != 0) newDir.setVisibility(View.GONE);
        page.addView(head);

        FrameLayout body = new FrameLayout(this);
        lv = new ListView(this);
        lv.setDivider(null);
        lv.setSelector(U.rect(0x4400B8D4, U.dp(this, 10), U.dp(this, 2), U.ACC));
        lv.setAdapter(ad);
        lv.setOnItemClickListener((a, v, i, id) -> open(items.get(i)));
        lv.setOnItemLongClickListener((a, v, i, id) -> { actions(items.get(i)); return true; });
        body.addView(lv, new FrameLayout.LayoutParams(-1, -1));
        empty = new TextView(this);
        empty.setTextColor(U.SUB); empty.setTextSize(20); empty.setGravity(Gravity.CENTER);
        body.addView(empty, new FrameLayout.LayoutParams(-1, -1));
        lv.setEmptyView(empty);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, 0, 1f);
        bp.setMargins(0, U.dp(this, 10), 0, U.dp(this, 6));
        page.addView(body, bp);

        count = new TextView(this);
        count.setTextColor(U.SUB); count.setTextSize(13);
        count.setText("Uzun bas / Menü tuşu: işlemler");
        page.addView(count);
        setContentView(page);
        load(null);
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    void sortItems() {
        Collections.sort(items, new Comparator<File>() {
            public int compare(File a, File b) {
                if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                if (sort == 1) return Long.compare(b.lastModified(), a.lastModified());
                if (sort == 2) return Long.compare(b.length(), a.length());
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
    }

    void done(File focus) {
        sortItems();
        ad.notifyDataSetChanged();
        int sel = focus == null ? -1 : items.indexOf(focus);
        lv.setSelection(sel < 0 ? 0 : sel);
        lv.requestFocus();
        empty.setText("Dosya yok");
        count.setText(items.size() + " öğe  •  Uzun bas / Menü tuşu: işlemler");
        paste.setVisibility(clip != null && cat == 0 ? View.VISIBLE : View.GONE);
    }

    void load(File focus) {
        if (cat == 0) {
            path.setText(cur.getPath());
            items.clear();
            File[] a = cur.listFiles();
            if (a != null) for (File f : a) if (!f.getName().startsWith(".")) items.add(f);
            done(focus);
        } else if (!scanning) {
            scanning = true;
            empty.setText("Taranıyor…");
            new Thread(() -> {
                ArrayList<File> r = new ArrayList<>();
                scan(Environment.getExternalStorageDirectory(), 0, r);
                runOnUiThread(() -> { scanning = false; items = r; done(null); });
            }).start();
        }
    }

    void scan(File d, int depth, ArrayList<File> out) {
        File[] a = d.listFiles();
        if (a == null) return;
        for (File f : a) {
            String n = f.getName();
            if (n.startsWith(".") || (depth == 0 && n.equals("Android"))) continue;
            if (f.isDirectory()) { if (depth < 12) scan(f, depth + 1, out); }
            else if (cat == 1 ? U.isImg(n) : cat == 2 ? U.isVid(n) : U.isDoc(n)) out.add(f);
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

    void open(File f) {
        String n = f.getName();
        if (f.isDirectory()) { cur = f; load(null); return; }
        if (U.isImg(n)) {
            ArrayList<File> l = new ArrayList<>(); int k = 0;
            for (File x : items) if (!x.isDirectory() && U.isImg(x.getName())) { if (x == f) k = l.size(); l.add(x); }
            ImgActivity.list = l; ImgActivity.idx = k;
            startActivity(new Intent(this, ImgActivity.class));
        } else if (U.isVid(n)) {
            ArrayList<File> l = new ArrayList<>(); int k = 0;
            for (File x : items) if (!x.isDirectory() && U.isVid(x.getName())) { if (x == f) k = l.size(); l.add(x); }
            VidActivity.list = l; VidActivity.idx = k;
            startActivity(new Intent(this, VidActivity.class));
        } else if (U.ext(n).equals("pdf")) {
            startActivity(new Intent(this, PdfActivity.class).putExtra("p", f.getPath()));
        } else {
            Intent in = new Intent(Intent.ACTION_VIEW);
            in.setDataAndType(FP.uri(f), FP.mime(f));
            in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try { startActivity(in); } catch (Exception e) { toast("Bu dosyayı açacak uygulama yok"); }
        }
    }

    AlertDialog.Builder dlg() { return new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK); }

    void actions(final File f) {
        String[] ops = {"Aç", "Yeniden adlandır", "Kopyala", "Taşı", "Sil", "Paylaş", "Bilgi"};
        dlg().setTitle(f.getName()).setItems(ops, (d, i) -> {
            switch (i) {
                case 0: open(f); break;
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
                else if (f.renameTo(t)) { if (cat == 0) load(t); else { items.set(items.indexOf(f), t); done(t); } }
                else toast("Yeniden adlandırılamadı");
            }).show();
    }

    void newFolder() {
        final EditText et = new EditText(this);
        et.setText("Yeni klasör"); et.setSingleLine(true);
        dlg().setTitle("Yeni klasör").setView(et).setNegativeButton("İptal", null)
            .setPositiveButton("Oluştur", (d, w) -> {
                File t = new File(cur, et.getText().toString().trim());
                if (t.exists() || !t.mkdirs()) toast("Oluşturulamadı"); else load(t);
            }).show();
    }

    void delete(final File f) {
        dlg().setTitle("Silinsin mi?").setMessage(f.getName()).setNegativeButton("İptal", null)
            .setPositiveButton("Sil", (d, w) -> new Thread(() -> {
                U.del(f);
                runOnUiThread(() -> {
                    if (clip == f) clip = null;
                    if (cat == 0) load(null); else { items.remove(f); done(null); }
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
                load(dest);
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
        dlg().setTitle("Sırala").setItems(new String[]{"Ada göre", "Tarihe göre (yeni)", "Boyuta göre"}, (d, i) -> { sort = i; done(null); }).show();
    }

    class Ad extends BaseAdapter {
        public int getCount() { return items.size(); }
        public Object getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return i; }
        public View getView(int i, View v, ViewGroup g) {
            Context c = BrowseActivity.this;
            TextView[] t;
            if (v == null) {
                LinearLayout r = new LinearLayout(c);
                r.setGravity(Gravity.CENTER_VERTICAL);
                r.setPadding(U.dp(c, 14), U.dp(c, 9), U.dp(c, 14), U.dp(c, 9));
                TextView ic = new TextView(c); ic.setTextSize(26); ic.setGravity(Gravity.CENTER);
                r.addView(ic, new LinearLayout.LayoutParams(U.dp(c, 52), -2));
                LinearLayout col = new LinearLayout(c); col.setOrientation(LinearLayout.VERTICAL);
                TextView nm = new TextView(c); nm.setTextSize(18); nm.setTextColor(U.TXT); nm.setSingleLine(true); nm.setEllipsize(TextUtils.TruncateAt.END);
                TextView sb = new TextView(c); sb.setTextSize(13); sb.setTextColor(U.SUB); sb.setSingleLine(true);
                col.addView(nm); col.addView(sb);
                r.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
                t = new TextView[]{ic, nm, sb};
                r.setTag(t); v = r;
            } else t = (TextView[]) v.getTag();
            File f = items.get(i);
            String n = f.getName();
            t[0].setText(f.isDirectory() ? "📁" : U.isImg(n) ? "🖼️" : U.isVid(n) ? "🎬" : U.ext(n).equals("pdf") ? "📕" : U.ext(n).equals("apk") ? "📦" : U.in(n, "mp3,wav,flac,m4a,ogg,aac") ? "🎵" : "📄");
            t[1].setText(n);
            String where = cat != 0 && f.getParentFile() != null ? f.getParentFile().getName() + "  •  " : "";
            t[2].setText(where + (f.isDirectory() ? "" : U.size(f.length()) + "  •  ") + U.date(f.lastModified()));
            return v;
        }
    }
}
