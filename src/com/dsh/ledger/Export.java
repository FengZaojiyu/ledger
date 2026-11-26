package com.dsh.ledger;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** 导出 CSV / XLSX / DOCX / PDF 到「下载/小账本」 */
public class Export {

    public static final String MIME_CSV = "text/csv";
    public static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String MIME_PDF = "application/pdf";

    public static String stamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmm", Locale.CHINA).format(new Date());
    }

    public static String dayStr(long ts) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date(ts));
    }

    // ---------------- CSV ----------------

    public static byte[] csv(List<Rec> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        sb.append("日期,类型,分类,备注,金额\r\n");
        for (Rec r : list) {
            sb.append(dayStr(r.ts)).append(',')
              .append(r.type == Db.EXPENSE ? "支出" : "收入").append(',')
              .append(esc(r.cat)).append(',')
              .append(esc(r.note)).append(',')
              .append(String.format(Locale.CHINA, "%.2f", r.amount)).append("\r\n");
        }
        return utf8(sb.toString());
    }

    // ---------------- XLSX ----------------

    public static byte[] xlsx(List<Rec> list, String title) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream z = new ZipOutputStream(bos);

        put(z, "[Content_Types].xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "</Types>");

        put(z, "_rels/.rels",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>");

        put(z, "xl/workbook.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
            + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets><sheet name=\"账单\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");

        put(z, "xl/_rels/workbook.xml.rels",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "</Relationships>");

        StringBuilder sh = new StringBuilder();
        sh.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sh.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        String[] head = {"日期", "类型", "分类", "备注", "金额(元)"};
        sh.append(row(1, head));
        int i = 2;
        for (Rec r : list) {
            sh.append(row(i++, new String[]{
                    dayStr(r.ts),
                    r.type == Db.EXPENSE ? "支出" : "收入",
                    r.cat == null ? "" : r.cat,
                    r.note == null ? "" : r.note,
                    String.format(Locale.CHINA, "%.2f", r.amount)}));
        }
        sh.append("</sheetData></worksheet>");
        put(z, "xl/worksheets/sheet1.xml", sh.toString());

        z.close();
        return bos.toByteArray();
    }

    private static String row(int r, String[] cells) {
        StringBuilder sb = new StringBuilder();
        sb.append("<row r=\"").append(r).append("\">");
        char col = 'A';
        for (String c : cells) {
            sb.append("<c r=\"").append(col).append(r).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
              .append(xml(c)).append("</t></is></c>");
            col++;
        }
        sb.append("</row>");
        return sb.toString();
    }

    // ---------------- DOCX ----------------

    public static byte[] docx(List<Rec> list, String title) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream z = new ZipOutputStream(bos);

        put(z, "[Content_Types].xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
            + "</Types>");

        put(z, "_rels/.rels",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
            + "</Relationships>");

        StringBuilder d = new StringBuilder();
        d.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        d.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");

        d.append("<w:p><w:r><w:rPr><w:b/><w:sz w:val=\"36\"/></w:rPr><w:t>")
         .append(xml(title)).append("</w:t></w:r></w:p>");
        d.append("<w:p><w:r><w:t>共 ").append(list.size()).append(" 条记录</w:t></w:r></w:p>");

        d.append("<w:tbl><w:tblPr><w:tblW w:w=\"0\" w:type=\"auto\"/>")
         .append("<w:tblBorders>")
         .append("<w:top w:val=\"single\" w:sz=\"4\" w:color=\"BFBFBF\"/>")
         .append("<w:left w:val=\"single\" w:sz=\"4\" w:color=\"BFBFBF\"/>")
         .append("<w:bottom w:val=\"single\" w:sz=\"4\" w:color=\"BFBFBF\"/>")
         .append("<w:right w:val=\"single\" w:sz=\"4\" w:color=\"BFBFBF\"/>")
         .append("<w:insideH w:val=\"single\" w:sz=\"4\" w:color=\"BFBFBF\"/>")
         .append("<w:insideV w:val=\"single\" w:sz=\"4\" w:color=\"BFBFBF\"/>")
         .append("</w:tblBorders></w:tblPr>");

        d.append(trow(new String[]{"日期", "类型", "分类", "备注", "金额"}, true));
        for (Rec r : list) {
            d.append(trow(new String[]{
                    dayStr(r.ts),
                    r.type == Db.EXPENSE ? "支出" : "收入",
                    r.cat == null ? "" : r.cat,
                    r.note == null ? "" : r.note,
                    String.format(Locale.CHINA, "%.2f", r.amount)}, false));
        }
        d.append("</w:tbl>");
        d.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>"
                + "<w:pgMar w:top=\"1134\" w:right=\"1134\" w:bottom=\"1134\" w:left=\"1134\"/></w:sectPr>");
        d.append("</w:body></w:document>");
        put(z, "word/document.xml", d.toString());

        z.close();
        return bos.toByteArray();
    }

    private static String trow(String[] cells, boolean bold) {
        StringBuilder sb = new StringBuilder("<w:tr>");
        for (String c : cells) {
            sb.append("<w:tc><w:p><w:r>");
            if (bold) sb.append("<w:rPr><w:b/></w:rPr>");
            sb.append("<w:t xml:space=\"preserve\">").append(xml(c)).append("</w:t>");
            sb.append("</w:r></w:p></w:tc>");
        }
        sb.append("</w:tr>");
        return sb.toString();
    }

    // ---------------- PDF ----------------

    public static byte[] pdf(List<Rec> list, String title) throws IOException {
        PdfDocument doc = new PdfDocument();
        int W = 595, H = 842, margin = 40;
        int y = margin;
        int pageNo = 1;

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(18);
        titlePaint.setFakeBoldText(true);

        Paint head = new Paint();
        head.setColor(0xFF333333);
        head.setTextSize(10.5f);
        head.setFakeBoldText(true);

        Paint body = new Paint();
        body.setColor(0xFF222222);
        body.setTextSize(10);

        Paint right = new Paint(body);
        right.setTextAlign(Paint.Align.RIGHT);

        PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo).create());
        Canvas cv = page.getCanvas();
        cv.drawText(title, margin, 60, titlePaint);
        cv.drawText("共 " + list.size() + " 条记录　生成于 " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date()),
                margin, 80, body);

        y = 110;
        int[] xs = {margin, margin + 92, margin + 150, margin + 220, W - margin};
        cv.drawText("日期", xs[0], y, head);
        cv.drawText("类型", xs[1], y, head);
        cv.drawText("分类", xs[2], y, head);
        cv.drawText("备注", xs[3], y, head);
        cv.drawText("金额", xs[4], y, right);
        y += 8;
        cv.drawLine(margin, y, W - margin, y, body);
        y += 18;

        for (Rec r : list) {
            if (y > H - 60) {
                doc.finishPage(page);
                pageNo++;
                page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo).create());
                cv = page.getCanvas();
                y = 70;
                cv.drawText("日期", xs[0], y, head);
                cv.drawText("类型", xs[1], y, head);
                cv.drawText("分类", xs[2], y, head);
                cv.drawText("备注", xs[3], y, head);
                cv.drawText("金额", xs[4], y, right);
                y += 8;
                cv.drawLine(margin, y, W - margin, y, body);
                y += 18;
            }
            cv.drawText(dayStr(r.ts), xs[0], y, body);
            cv.drawText(r.type == Db.EXPENSE ? "支出" : "收入", xs[1], y, body);
            cv.drawText(clip(r.cat, 4), xs[2], y, body);
            cv.drawText(clip(r.note, 14), xs[3], y, body);
            cv.drawText(String.format(Locale.CHINA, "%.2f", r.amount), xs[4], y, right);
            y += 20;
        }
        doc.finishPage(page);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.writeTo(bos);
        doc.close();
        return bos.toByteArray();
    }

    private static String clip(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    // ---------------- 保存 ----------------

    /** 保存到 下载/小账本/ ，返回可读路径描述 */
    public static String save(Context ctx, String name, String mime, byte[] data) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/小账本");
            Uri uri = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new IOException("无法创建文件");
            OutputStream os = ctx.getContentResolver().openOutputStream(uri);
            os.write(data);
            os.close();
            return "下载/小账本/" + name;
        }
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "小账本");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, name);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(data);
        fos.close();
        return f.getAbsolutePath();
    }

    // ---------------- 工具 ----------------

    private static void put(ZipOutputStream z, String name, String content) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(utf8(content));
        z.closeEntry();
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (Exception e) {
            return s.getBytes();
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        if (s.indexOf(',') < 0 && s.indexOf('"') < 0 && s.indexOf('\n') < 0) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String xml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
