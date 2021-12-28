package darwin;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.util.ArrayList;
import java.util.List;


public class DoublePlot{
    private final LineChart<Number, Number> lineChart;
    private int firstDayInPlot = 0;
    private static final int maxPoints = 25;
    private final NumberAxis xAxis = new NumberAxis();
    private final XYChart.Series<Number, Number> series1 = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> series2 = new XYChart.Series<>();
    private final List<Number> series1AllValues = new ArrayList<>();
    private final List<Number> series2AllValues = new ArrayList<>();

    public DoublePlot(String series1Name, String series2Name, Number firstValueSeries1, Number firstValueSeries2){
        NumberAxis yAxis = new NumberAxis();
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
        series1AllValues.add(firstSeriesValue);
        series2AllValues.add(secondSeriesValue);

        if (series1.getData().size() > maxPoints){
            firstDayInPlot++;
            series1.getData().remove(0);
            series2.getData().remove(0);
            xAxis.setLowerBound(firstDayInPlot);
            xAxis.setUpperBound(maxPoints+firstDayInPlot);
        }
    }

    public LineChart<Number, Number> getLineChart() {
        return lineChart;
    }

    public List<Number> getSeries1AllValues() {
        return series1AllValues;
    }

    public List<Number> getSeries2AllValues() {
        return series2AllValues;
    }

    public Number getSeries1Average(){
        return series1AllValues.stream().mapToDouble(Number::doubleValue).sum() / series1AllValues.size();
    }

    public Number getSeries2Average(){
        return series2AllValues.stream().mapToDouble(Number::doubleValue).sum() / series2AllValues.size();
    }
}
