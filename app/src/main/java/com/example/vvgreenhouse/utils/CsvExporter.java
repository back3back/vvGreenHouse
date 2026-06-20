package com.example.vvgreenhouse.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CSV 导出工具类
 *
 * 将任意二维数据导出为 CSV 文件到 Downloads 目录，
 * 并通过 FileProvider 分享。
 */
public class CsvExporter {

    /**
     * 导出 CSV 到 Downloads 并返回文件对象
     *
     * @param folder   子目录名（如 "GreenHouse"）
     * @param prefix   文件名前缀（如 "sales_export"）
     * @param headers  表头列名列表
     * @param rows     数据行列表（每行是一条记录的 String[]）
     * @return 导出的 File，失败返回 null
     */
    public static File export(String folder, String prefix, String[] headers, List<String[]> rows) {
        String timeStr = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), folder);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, prefix + "_" + timeStr + ".csv");
        try (FileWriter fw = new FileWriter(file)) {
            // BOM for Excel UTF-8 compatibility
            fw.write('﻿');
            // Header
            fw.write(join(headers, ",") + "\n");
            // Data rows
            for (String[] row : rows) {
                fw.write(escapeRow(row) + "\n");
            }
            fw.flush();
            return file;
        } catch (IOException e) {
            return null;
        }
    }

    /** 分享文件：通过 FileProvider + Intent */
    public static void share(Context ctx, File file, String mimeType) {
        try {
            android.net.Uri uri = FileProvider.getUriForFile(
                    ctx, ctx.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(intent, "分享文件"));
        } catch (Exception e) {
            Toast.makeText(ctx, "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ========== helpers ==========

    private static String join(String[] parts, String delim) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(delim);
            sb.append(parts[i] != null ? parts[i] : "");
        }
        return sb.toString();
    }

    private static String escapeRow(String[] row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length; i++) {
            if (i > 0) sb.append(",");
            String val = (row[i] != null ? row[i] : "");
            // Escape: wrap in quotes if contains comma, quote or newline
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                val = "\"" + val.replace("\"", "\"\"") + "\"";
            }
            sb.append(val);
        }
        return sb.toString();
    }
}
