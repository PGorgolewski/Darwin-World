package darwin;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.util.ArrayList;
import java.util.List;

public class Plot {
    private final LineChart<Number, Number> lineChart;
    private static final int maxPoints = 25;
    private int firstDayInPlot = 0;
    private final NumberAxis xAxis = new NumberAxis();
    private final XYChart.Series<Number, Number> series = new XYChart.Series<>();
    private final List<Number> allValues = new ArrayList<>();

    public Plot(String seriesName, Number firstValueOfSeries){
        NumberAxis yAxis = new NumberAxis();
        lineChart = new LineChart<>(xAxis, yAxis);
        series.setName(seriesName);
        lineChart.getData().add(series);
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(maxPoints);
        updatePlot(0, firstValueOfSeries);
    }

    public void updatePlot(int day, Number value){
        series.getData().add(new XYChart.Data<>(day, value));
        allValues.add(value);

        if (series.getData().size() > maxPoints){
            firstDayInPlot++;
            series.getData().remove(0);
            xAxis.setLowerBound(firstDayInPlot);
            xAxis.setUpperBound(maxPoints+firstDayInPlot);
        }
    }

    public LineChart<Number, Number> getLineChart() {
        return lineChart;
    }

    public List<Number> getAllValues() {
        return allValues;
    }

    public Number getSeriesAverage(){
        return allValues.stream().mapToDouble(Number::doubleValue).sum() / allValues.size();
    }
}
