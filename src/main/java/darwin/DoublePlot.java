package darwin;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DoublePlot{
    private NumberAxis xAxis = new NumberAxis();
    private NumberAxis yAxis = new NumberAxis();
    private final XYChart.Series<Number, Number> series1 = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> series2 = new XYChart.Series<>();
    private int firstDayInPlot = 0;
    private static final int maxPoints = 25;
    private final LineChart<Number, Number> lineChart;

    public DoublePlot(String series1Name, String series2Name, Number firstValueSeries1, Number firstValueSeries2){
        lineChart = new LineChart<>(xAxis, yAxis);
        series1.setName(series1Name);
        series2.setName(series2Name);
        lineChart.getData().addAll(series1, series2);
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(maxPoints);
        updatePlot(0,firstValueSeries1,firstValueSeries2);
    }

    public void updatePlot(int day, Number firstSeriesValue, Number secondSeriesValue){
        series1.getData().add(new XYChart.Data<>(day, firstSeriesValue));
        series2.getData().add(new XYChart.Data<>(day, secondSeriesValue));

        if (series1.getData().size() > maxPoints){
            series1.getData().remove(0);
            series2.getData().remove(0);
            firstDayInPlot++;
            xAxis.setLowerBound(firstDayInPlot);
            xAxis.setUpperBound(maxPoints+firstDayInPlot);
        }
    }

    public LineChart<Number, Number> getLineChart() {
        return lineChart;
    }
}
