/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.client.gui.chart;

import org.apache.commons.lang3.time.DateUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.RangeType;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author lzf abc123lzf@126.com
 * @since 2020/12/24 1:47
 */
public final class DynamicTimeSeriesChart {

    private static final Logger log = LoggerFactory.getLogger(DynamicTimeSeriesChart.class);
    /**
     * 蓝色主题
     */
    public static final int STYLE_BLUE = 0;

    /**
     * 紫色主题
     */
    public static final int STYLE_PURPLE = 1;

    /**
     * JFreeChart图例
     */
    private final JFreeChart chart;

    /**
     * 最大时间间隔，单位秒
     */
    private final int interval;

    /**
     * 动态数据集
     */
    private final DynamicTimeSeriesCollection dataset;


    public DynamicTimeSeriesChart(String title, String xAxisName, String yAxisName, int interval, int style) {
        DynamicTimeSeriesCollection dataset = new DynamicTimeSeriesCollection(1, interval, new Second());
        dataset.setTimeBase(new Second(DateUtils.addSeconds(new Date(), -interval)));
        dataset.addSeries(new float[0], 0, "");

        JFreeChart chart = ChartFactory.createTimeSeriesChart("", xAxisName, yAxisName, dataset, false, false, false);
        chart.setTitle(new TextTitle(title, new Font("黑体", Font.PLAIN, 18)));
        if (style == STYLE_BLUE) {
            initialChartStyle(chart, Color.white, new Color(17, 125, 187),
                    new Color(217, 234, 244),
                    new Color(217, 231, 241),
                    new Color(75, 157, 203));
        } else if (style == STYLE_PURPLE) {
            initialChartStyle(chart, Color.white, new Color(139, 18, 174),
                    new Color(236, 222, 240),
                    new Color(244, 242, 244),
                    new Color(149, 40, 180));
        } else {
            throw new IllegalArgumentException("style");
        }

        this.interval = interval;
        this.dataset = dataset;
        this.chart = chart;
    }

    /**
     * 初始化图表样式
     * @param chart JFreeChart实例
     * @param background 背景颜色
     * @param outline 图标四周框颜色
     * @param gridline 坐标线颜色
     * @param line 折线颜色
     * @param fill 折线下方填充的颜色
     */
    private static void initialChartStyle(JFreeChart chart, Paint background, Paint outline, Paint gridline, Paint line, Paint fill) {
        chart.setBackgroundPaint(Color.white);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(background);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(gridline);
        plot.setDomainGridlineStroke(new BasicStroke(1.5f));

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(gridline);
        plot.setRangeGridlineStroke(new BasicStroke(1.5f));

        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setOutlinePaint(outline);

        XYAreaRenderer renderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        renderer.setDefaultFillPaint(line);
        renderer.setDefaultOutlinePaint(fill);
        renderer.setSeriesPaint(0, line);
        renderer.setUseFillPaint(true);
        renderer.setOutline(true);
        plot.setRenderer(renderer);

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("    "));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRangeType(RangeType.POSITIVE);
        plot.getRangeAxis().setAutoRange(true);
        plot.getRangeAxis().setAutoRangeMinimumSize(0.2);

    }


    public void resetData() {
        dataset.setTimeBase(new Second(DateUtils.addSeconds(new Date(), -interval)));
        dataset.addSeries(new float[0], 0, "");
    }

    public void appendData(float data) {
        dataset.advanceTime();
        dataset.appendData(new float[] {data});
    }


    public JPanel buildPanel() {
        return new ChartPanel(chart);
    }


    public BufferedImage parseImage(int width, int height) {
        return null;
    }

}
