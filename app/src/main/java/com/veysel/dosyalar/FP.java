package com.veysel.dosyalar;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;

public class FP extends ContentProvider {
    static final String AUTH = "com.veysel.dosyalar.fp";

    static Uri uri(File f) { return Uri.parse("content://" + AUTH + Uri.encode(f.getAbsolutePath(), "/")); }
    static String mime(File f) {
        String m = MimeTypeMap.getSingleton().getMimeTypeFromExtension(U.ext(f.getName()));
        return m == null ? "*/*" : m;
    }

    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri u) { return mime(new File(u.getPath())); }
    @Override public ParcelFileDescriptor openFile(Uri u, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(new File(u.getPath()), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) {
        File f = new File(u.getPath());
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{f.getName(), f.length()});
        return c;
    }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
