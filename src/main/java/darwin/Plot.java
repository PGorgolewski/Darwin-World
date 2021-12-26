package darwin;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class Plot {
    private NumberAxis xAxis = new NumberAxis();
    private NumberAxis yAxis = new NumberAxis();
    private final XYChart.Series<Number, Number> series = new XYChart.Series<>();
    private static final int maxPoints = 25;
    private int firstDayInPlot = 0;
    private final LineChart<Number, Number> lineChart;

    public Plot(String seriesName, Number firstValueOfSeries){
        lineChart = new LineChart<>(xAxis, yAxis);
        series.setName(seriesName);
        lineChart.getData().add(series);
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(maxPoints);
        updatePlot(0, firstValueOfSeries);
    }

    public void updatePlot(int day, Number firstValueOfSeries){
        series.getData().add(new XYChart.Data<>(day, firstValueOfSeries));
        if (series.getData().size() > maxPoints){
            series.getData().remove(0);
            firstDayInPlot++;
            xAxis.setLowerBound(firstDayInPlot);
            xAxis.setUpperBound(maxPoints+firstDayInPlot);
        }
    }

    public LineChart<Number, Number> getLineChart() {
        return lineChart;
    }
}
