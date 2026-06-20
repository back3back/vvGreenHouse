package com.example.vvgreenhouse.widget;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义简易折线图控件
 *
 * 纯 Canvas 绘制，零外部依赖。
 * 支持多条折线、Y轴标注、时间轴标签、数据点圆圈标记。
 */
public class SimpleLineChart extends View {

    // ========== 绘制组件 ==========
    private final Paint paintAxis = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ========== 数据 ==========
    private final List<Series> seriesList = new ArrayList<>();

    // ========== 边距 ==========
    private int paddingLeft = 80;
    private int paddingRight = 30;
    private int paddingTop = 30;
    private int paddingBottom = 60;

    // ========== 颜色 ==========
    private int gridColor = 0xFFE0E0E0;
    private int axisColor = 0xFF616161;
    private int textColor = 0xFF424242;

    public SimpleLineChart(Context context) {
        super(context);
        init();
    }

    public SimpleLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintAxis.setColor(axisColor);
        paintAxis.setStrokeWidth(2f);
        paintAxis.setStyle(Paint.Style.STROKE);

        paintGrid.setColor(gridColor);
        paintGrid.setStrokeWidth(1f);
        paintGrid.setStyle(Paint.Style.STROKE);

        paintLine.setStrokeWidth(3f);
        paintLine.setStyle(Paint.Style.STROKE);

        paintDot.setStyle(Paint.Style.FILL);

        paintText.setColor(textColor);
        paintText.setTextSize(28f);
        paintText.setTextAlign(Paint.Align.CENTER);

        paintFill.setStyle(Paint.Style.FILL);
    }

    /** 添加一条数据折线 */
    public void addSeries(List<Float> values, List<String> labels, int lineColor, String name) {
        seriesList.clear();
        Series s = new Series();
        s.values = values;
        s.labels = labels;
        s.lineColor = lineColor;
        s.name = name;
        seriesList.add(s);
        invalidate();
    }

    /** 清空所有数据 */
    public void clear() {
        seriesList.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (seriesList.isEmpty()) return;

        Series series = seriesList.get(0);
        if (series.values == null || series.values.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();
        int chartLeft = paddingLeft;
        int chartRight = w - paddingRight;
        int chartTop = paddingTop;
        int chartBottom = h - paddingBottom;
        int chartWidth = chartRight - chartLeft;
        int chartHeight = chartBottom - chartTop;

        float minVal = Float.MAX_VALUE, maxVal = Float.MIN_VALUE;
        for (Series s : seriesList) {
            for (float v : s.values) {
                if (v < minVal) minVal = v;
                if (v > maxVal) maxVal = v;
            }
        }
        // 扩展10%范围避免折线顶天立地
        float range = maxVal - minVal;
        if (range < 1f) range = 1f;
        minVal -= range * 0.1f;
        maxVal += range * 0.1f;

        // ===== 网格 & Y 轴 =====
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            float y = chartTop + chartHeight * i / (float) gridLines;
            float val = maxVal - (maxVal - minVal) * i / (float) gridLines;
            // 网格线
            canvas.drawLine(chartLeft, y, chartRight, y, paintGrid);
            // Y 轴标签
            paintText.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.format("%.1f", val), chartLeft - 12, y + 10, paintText);
        }

        // ===== X 轴标签 & 外框 =====
        canvas.drawRect(chartLeft, chartTop, chartRight, chartBottom, paintAxis);
        int n = series.values.size();

        // X 轴标签步长
        int step = Math.max(1, n / 6);
        for (int i = 0; i < n; i += step) {
            float x = chartLeft + chartWidth * i / (float) (n - 1 > 0 ? n - 1 : 1);
            paintText.setTextAlign(Paint.Align.CENTER);
            String label = (series.labels != null && i < series.labels.size())
                    ? series.labels.get(i) : String.valueOf(i + 1);
            // 截断时间标签只保留 HH:mm
            if (label.length() > 5) label = label.substring(label.length() - 5);
            canvas.drawText(label, x, chartBottom + 40, paintText);
        }

        // ===== 每条折线 =====
        for (Series s : seriesList) {
            paintLine.setColor(s.lineColor | 0xFF000000);
            paintDot.setColor(s.lineColor | 0xFF000000);

            Path path = new Path();
            boolean first = true;
            float[] pts = new float[n * 2];

            for (int i = 0; i < n; i++) {
                float x = chartLeft + chartWidth * i / (float) (n - 1 > 0 ? n - 1 : 1);
                float ratio = (s.values.get(i) - minVal) / (maxVal - minVal);
                float y = chartBottom - ratio * chartHeight;
                pts[i * 2] = x;
                pts[i * 2 + 1] = y;

                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }

            // 半透明填充区域
            Path fillPath = new Path(path);
            fillPath.lineTo(pts[(n - 1) * 2], chartBottom);
            fillPath.lineTo(pts[0], chartBottom);
            fillPath.close();
            paintFill.setColor((s.lineColor & 0x00FFFFFF) | 0x20000000);
            paintFill.setStyle(Paint.Style.FILL);
            canvas.drawPath(fillPath, paintFill);

            // 折线
            canvas.drawPath(path, paintLine);

            // 数据点圆圈
            for (int i = 0; i < n; i++) {
                canvas.drawCircle(pts[i * 2], pts[i * 2 + 1], 6f, paintDot);
            }
        }
    }

    /** 数据序列 */
    public static class Series {
        public List<Float> values;
        public List<String> labels;
        public int lineColor;
        public String name;
    }
}
