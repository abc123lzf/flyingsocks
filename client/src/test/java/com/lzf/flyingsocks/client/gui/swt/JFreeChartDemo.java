package com.lzf.flyingsocks.client.gui.swt;

import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.swt.ChartComposite;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JFreeChartDemo {

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
        ChartComposite composite = new ChartComposite(canvas, SWT.NONE, chart);

        display.timerExec(1000, new Runnable() {
            @Override
            public void run() {
                demo.timeSeriesCollection.advanceTime();
                demo.timeSeriesCollection.appendData(new float[] {(float) (Math.random() * 100.0)});
                composite.forceRedraw();
                display.timerExec(1000, this);
            }
        });

        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                 display.sleep();
        }
    }


    private static DynamicTimeSeriesCollection createDataset() {
        DynamicTimeSeriesCollection dataset = new DynamicTimeSeriesCollection(1, 60, new Second());
        dataset.setTimeBase(new Second(DateUtils.addMinutes(new Date(), -1)));
        dataset.addSeries(new float[0], 0, "test");
        return dataset;
    }

    private static JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "UPLOAD",  // title
                "TIME",             // x-axis label
                "MB/s",   // y-axis label
                dataset,            // data
                false,               // create legend?
                false,               // generate tooltips?
                false               // generate URLs?
        );

        chart.setBackgroundPaint(Color.white);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(new Color(217, 234, 244));
        plot.setDomainGridlineStroke(new BasicStroke(1.5f));

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(new Color(217, 234, 244));
        plot.setRangeGridlineStroke(new BasicStroke(1.5f));

        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setOutlinePaint(new Color(17, 125, 187));

        XYAreaRenderer renderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        //XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setOutline(true);

        renderer.setBaseOutlinePaint(new Color(75, 157, 203));
        renderer.setSeriesFillPaint(0, new Color(75, 157, 203));
        renderer.setSeriesPaint(0, new Color(217, 231, 241));

        plot.setRenderer(renderer);

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        plot.getRangeAxis().setAutoRangeMinimumSize(0.1);
        axis.setDateFormatOverride(new SimpleDateFormat("mm:ss"));

        return chart;

    }
}
