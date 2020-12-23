package com.lzf.flyingsocks.client.gui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.swt.ChartComposite;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Random;

public class JFreeChartDemo {

    private static int id = 0;

    private final DynamicTimeSeriesCollection timeSeriesCollection;

    public JFreeChartDemo(DynamicTimeSeriesCollection timeSeriesCollection) {
        this.timeSeriesCollection = timeSeriesCollection;
    }


    public static void main(String[] args) throws Exception {
        Display display = Display.getDefault();
        Shell shell = new Shell(display);
        shell.setSize(600, 300);
        //shell.setLayout(new FillLayout());
        shell.setText("Time Serials demo");

        JFreeChartDemo demo = new JFreeChartDemo(createDataset());
        JFreeChart chart = createChart(demo.timeSeriesCollection);

        Canvas canvas = Utils.createCanvas(shell, chart.createBufferedImage(600, 300), 0, 0, 600, 300);

        display.timerExec(1000, new Runnable() {
            @Override
            public void run() {
                demo.timeSeriesCollection.advanceTime();
                demo.timeSeriesCollection.appendData(new float[] {(float) (Math.random() * 100.0)});
                Utils.refreshCanvas(canvas, chart.createBufferedImage(600, 300));
                display.timerExec(1000, this);
            }
        });

        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                 display.sleep();
        }
    }

    private static void writeImage(JFreeChart chart) throws Exception {
        BufferedImage image = chart.createBufferedImage(800, 400);
        ImageIO.write(image, "png", new File("/tmp/chart" + id + ".png"));
        id++;
    }


    private static DynamicTimeSeriesCollection createDataset() {
        DynamicTimeSeriesCollection dataset = new DynamicTimeSeriesCollection(1, 180, new Second());
        dataset.setTimeBase(new Second());
        dataset.addSeries(new float[180], 0, "test");
        return dataset;
    }

    private static JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "网络流量监控",  // title
                "时间",             // x-axis label
                "速度(KB/s)",   // y-axis label
                dataset,            // data
                false,               // create legend?
                false,               // generate tooltips?
                false               // generate URLs?
        );

        chart.setBackgroundPaint(Color.white);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setBaseShapesVisible(true);
            renderer.setBaseShapesFilled(true);
        }

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        plot.getRangeAxis().setAutoRangeMinimumSize(1);
        axis.setDateFormatOverride(new SimpleDateFormat("mm分ss秒"));

        return chart;

    }
}
