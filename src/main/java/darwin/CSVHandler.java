package darwin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVHandler {
    String fileName;
    private final Map<String, List<Number>> allData = new HashMap<>();
    private final Map<String, Number> averages = new HashMap<>();

    public CSVHandler(String fileName){
        this.fileName = fileName + ".csv";
    }

    public void updateData(String dataName, List<Number> data, Number average){
        allData.put(dataName, data);
        averages.put(dataName, average);
    }

    public void createCSV() throws IOException {
        File file = new File(fileName);
        file.createNewFile();
        List<String> csvLines = createCSVLines();
        PrintWriter writer = new PrintWriter(fileName);
        csvLines.forEach(writer::println);
        writer.close();
    }

    public List<String> createCSVLines(){
        List<String> orderList = new ArrayList<>(new ArrayList<>(allData.keySet()));
        List<String> csvLines = new ArrayList<>();

        csvLines.add(createLine(orderList));

        for (int i=0; i<allData.get(orderList.get(0)).size(); i++){
            List<String> csvStrings = new ArrayList<>();
            for (String name: orderList)
                csvStrings.add(allData.get(name).get(i).toString());

            csvLines.add(createLine(csvStrings));
        }

        List<String> csvAverages = new ArrayList<>();
        for (String name: orderList)
            csvAverages.add(averages.get(name).toString());
        csvLines.add(createLine(csvAverages));

        return csvLines;
    }

    public String createLine(List<String> orderedStrings){
        return String.join(",", orderedStrings);
    }
}
