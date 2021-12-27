package darwin;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.*;

//TODO autoresizing
//TODO wskazywanie pojedynczego zwierza po zatrzymaniu
//TODO zapisywanie statystyk do pliku w csv formacie
//TODO ma byc uruchamiane za pomoca komendy gradla
public class App extends Application implements IAppObserver {
    GridPane snakeGrid = new GridPane();
    GridPane wallGrid = new GridPane();
    SimulationEngine snakeEngine;
    SimulationEngine wallEngine;
    private boolean ifMagicBorn;
    private int startEnergy;
    private final Map<String, String> defaultMenuValues = createDefaultMenuValues();
    private final Map<String, TextField> menuTextFields = new HashMap<>();
    private Scene menuScene;
    private DoublePlot snakeDoublePlot;
    private DoublePlot wallDoublePlot;
    private final Label snakeGenotype = new Label();
    private final Label wallGenotype = new Label();
    private final Label snakeMagicBorn = new Label("Magic born 0/3");
    private final Label wallMagicBorn = new Label("Magic born 0/3");
    private final Map<String, Plot> wallPlots = new HashMap<>();
    private final Map<String, Plot> snakePlots = new HashMap<>();
    private Thread engineSnakeThread;
    private Thread engineWallThread;


    public void addPlots(Map<String, Plot> givenMap){
        givenMap.put("Average energy", new Plot("Average energy", startEnergy));
        givenMap.put("Average lifetime", new Plot("Average lifetime", 0));
        givenMap.put("Average children number", new Plot("Average children number", 0));
    }

    public void start(Stage primaryStage) {
        Scene menuScene = createMenuScene(primaryStage);
        this.menuScene = menuScene;
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.setScene(menuScene);
        primaryStage.setTitle("Darwin World Simulation");
        primaryStage.show();
    }

    public Scene createMenuScene(Stage primaryStage){
        return new Scene(createVBoxMenu(primaryStage));
    }

    public VBox createVBoxMenu(Stage primaryStage){
        Button createSimulationButton = new Button("Create Simulation");

        createSimulationButton.setOnAction(click -> {
            Map<String, Number> menuArgs = null;
            try {
                menuArgs = OptionParser.parseArguments(menuTextFields);
            } catch (Exception e) {
                e.printStackTrace();
            }
            clearGrid(wallGrid);
            clearGrid(snakeGrid);
            Scene simulationScene = createSimulationScene(menuArgs, primaryStage);
            primaryStage.setScene(simulationScene);
            engineSnakeThread.start();
            engineWallThread.start();
        });

        Button exitButton = new Button("Exit");

        exitButton.setOnAction(click -> {
            Platform.exit();
            System.exit(0);
        });

        HBox buttonBox = new HBox(40, createSimulationButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);
        return new VBox(40,
                createVBoxWithLabelsAndTextFields("Map Properties",
                        new String[]{"Map height", "Map width", "Jungle ratio"}),
                createVBoxWithLabelsAndTextFields("Menu Elements Properties",
                        new String[]{"Grass energy", "Animal start energy", "Animal move energy"}),
                createVBoxWithLabelsAndTextFields("Others",
                        new String[]{"Start animals number", "Refresh time (in ms)", "Use magic born [yes/no]"}),
                buttonBox);
    }

    public Scene createSimulationScene(Map<String, Number> menuArgs, Stage primaryStage){
        prepareSimulation(menuArgs);
        prepareGrid(snakeGrid, snakeEngine.getMap());
        prepareGrid(wallGrid, wallEngine.getMap());

        engineSnakeThread = new Thread(snakeEngine);
        engineWallThread = new Thread(wallEngine);

        HBox startStopSnakeBox = prepareStartStopBox(snakeEngine);
        HBox startStopWallBox = prepareStartStopBox(wallEngine);

        VBox snakeStatsVBox = new VBox(40, snakeGrid, startStopSnakeBox,
                prepareGenotypeDominantAndMagicBornInfo(snakeGenotype, snakeMagicBorn, snakeEngine.getMap()),
                this.prepareAllPlotsVBox(snakeDoublePlot, snakePlots));
        VBox wallStatsVBox = new VBox(40, wallGrid, startStopWallBox,
                prepareGenotypeDominantAndMagicBornInfo(wallGenotype, wallMagicBorn, wallEngine.getMap()),
                this.prepareAllPlotsVBox(wallDoublePlot, wallPlots));

        HBox simulationsBox = new HBox(40, snakeStatsVBox, wallStatsVBox);
        VBox sceneBox = new VBox(40, createMapLegendHBox(), simulationsBox, createExitButtonHBox(primaryStage));
        simulationsBox.setAlignment(Pos.CENTER);
        sceneBox.setAlignment(Pos.CENTER);
        snakeGrid.setAlignment(Pos.CENTER);
        wallGrid.setAlignment(Pos.CENTER);
        return new Scene(sceneBox);
    }

    public HBox createExitButtonHBox(Stage primaryStage){
        Button exitButton = new Button("Exit to menu");

        exitButton.setOnAction(click -> {
            engineSnakeThread.stop();
            engineWallThread.stop();
            primaryStage.setScene(menuScene);
        });

        HBox hbox = new HBox(exitButton);
        hbox.setAlignment(Pos.CENTER);
        return hbox;
    }

    public HBox createMapLegendHBox(){
        Color[] colors = {Color.LIGHTGREEN, Color.LIME, Color.GREEN};
        String[] descriptions = {"Step Field", "Grass Field", "Jungle Field"};
        HBox hBox = new HBox(40);

        for (int i=0; i < colors.length; i++){
            HBox oneLegendField = new HBox(20, new Rectangle(10,10, colors[i]), new Label(descriptions[i]));
            oneLegendField.setAlignment(Pos.CENTER);
            hBox.getChildren().add(oneLegendField);
        }

        HBox animalLegend = new HBox(20, new Circle(5, Color.SADDLEBROWN), new Label("Animal (the darker the stronger)"));
        animalLegend.setAlignment(Pos.CENTER);
        hBox.getChildren().add(animalLegend);
        hBox.setAlignment(Pos.CENTER);
        return hBox;
    }

    public void prepareSimulation(Map<String, Number> menuArgs){
        int width = (int) menuArgs.get("Map width");
        int height = (int) menuArgs.get("Map height");
        float jungleRatio = (float) menuArgs.get("Jungle ratio");
        int startAnimalsNumber = (int) menuArgs.get("Start animals number");
        int grassEnergy = (int) menuArgs.get("Grass energy");
        int moveEnergy = (int) menuArgs.get("Animal move energy");
        this.ifMagicBorn = (int) menuArgs.get("Use magic born [yes/no]") == 1;
        this.startEnergy = (int) menuArgs.get("Animal start energy");
        int delay = (int) menuArgs.get("Refresh time (in ms)");

        this.snakeEngine = new SimulationEngine(width, height, jungleRatio, startAnimalsNumber,
                startEnergy, moveEnergy, grassEnergy, delay, ifMagicBorn,false, this);
        this.wallEngine = new SimulationEngine(width, height, jungleRatio, startAnimalsNumber,
                startEnergy, moveEnergy, grassEnergy, delay, ifMagicBorn, true, this);

        this.snakeDoublePlot = new DoublePlot("Animals","Grasses", startAnimalsNumber, 0);
        this.wallDoublePlot = new DoublePlot("Animals","Grasses", startAnimalsNumber, 0);
        
        this.addPlots(this.wallPlots);
        this.addPlots(this.snakePlots);
    }

    public VBox createVBoxWithLabelsAndTextFields(String primaryLabel, String[] textFieldNames){
        Label label = new Label(primaryLabel);
        label.setStyle("-fx-font-weight: bold");
        VBox vbox = new VBox(20, label, createLabelsWithTextFields(textFieldNames));
        vbox.setAlignment(Pos.CENTER);
        return vbox;
    }

    public VBox createLabelsWithTextFields(String[] properties){
        VBox vBox = new VBox(30);
        vBox.setAlignment(Pos.CENTER);
        for (String oneProperty: properties){
            Label label = new Label(oneProperty);
            label.setStyle("-fx-font-weight: bold");
            label.setAlignment(Pos.CENTER);

            TextField textField = new TextField(defaultMenuValues.get(oneProperty));
            textField.setAlignment(Pos.CENTER);

            menuTextFields.put(oneProperty, textField);
            HBox hbox = new HBox(20, label, textField);
            hbox.setAlignment(Pos.CENTER);
            vBox.getChildren().add(hbox);
        }
        return vBox;
    }

    public Map<String,String> createDefaultMenuValues(){
        Map<String,String> defaultMenuValues = new HashMap();
        defaultMenuValues.put("Map height", "10");
        defaultMenuValues.put("Map width", "10");
        defaultMenuValues.put("Jungle ratio", "0.5");
        defaultMenuValues.put("Grass energy", "30");
        defaultMenuValues.put("Animal start energy", "200");
        defaultMenuValues.put("Animal move energy", "5");
        defaultMenuValues.put("Use magic born [yes/no]", "no");
        defaultMenuValues.put("Start animals number", "20");
        defaultMenuValues.put("Refresh time (in ms)", "300");

        return defaultMenuValues;
    }

    public HBox prepareGenotypeDominantAndMagicBornInfo(Label givenGenotypeLabel, Label givenMagicBornLabel, AbstractMap map){
        givenGenotypeLabel.setText("Dominant genotype: " + map.getTheMostFrequentGenotype().toString());
        HBox hBox;
        if (ifMagicBorn)
            hBox = new HBox(20, givenGenotypeLabel, givenMagicBornLabel);
        else
            hBox = new HBox(20, givenGenotypeLabel);
        hBox.setAlignment(Pos.CENTER);
        return hBox;
    }

    public VBox prepareAllPlotsVBox(DoublePlot givenDoublePlot, Map<String, Plot> givenPlots){
        return new VBox(40, preparePlotHBox(givenDoublePlot, givenPlots.get("Average energy")),
                preparePlotHBox(givenPlots.get("Average lifetime"), givenPlots.get("Average children number")));
    }

    public HBox preparePlotHBox(DoublePlot doublePlot, Plot plot){
       return new HBox(20, doublePlot.getLineChart(), plot.getLineChart());
    }

    public HBox preparePlotHBox(Plot firstPlot, Plot secondPlot){
        return new HBox(20, firstPlot.getLineChart(), secondPlot.getLineChart());
    }

    public HBox prepareStartStopBox(SimulationEngine givenSimulation){
        HBox box = new HBox(20, prepareStartButton(givenSimulation),
                prepareStopButton(givenSimulation));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    public Button prepareStopButton(SimulationEngine givenSimulation){
        Button button = new Button("Stop Simulation");

        button.setOnAction(click -> {
            givenSimulation.setIfRunning(false);
        });

        return button;
    }

    public Button prepareStartButton(SimulationEngine givenSimulation){
        Button button = new Button("Start Simulation");

        button.setOnAction(click -> {
            givenSimulation.setIfRunning(true);
        });

        return button;
    }

    public void show(AbstractMap map, int day, int animalsNumber, int grassNumber, float averageEnergy,
                     float averageLifetime, float averageChildrenNumber, int magicBornCounter) {
        GridPane grid;
        DoublePlot doublePlot;
        Map<String, Plot> plots;
        Label magicBorn;
        Label genotype;
        if (map instanceof WallMap) {
            grid = wallGrid;
            doublePlot = wallDoublePlot;
            plots = wallPlots;
            magicBorn = wallMagicBorn;
            genotype = wallGenotype;
        }
        else {
            grid = snakeGrid;
            doublePlot = snakeDoublePlot;
            plots = snakePlots;
            magicBorn = snakeMagicBorn;
            genotype = snakeGenotype;
        }

        Platform.runLater(() -> {
            clearGrid(grid);
            prepareGrid(grid, map);
            doublePlot.updatePlot(day,animalsNumber,grassNumber);
            plots.get("Average energy").updatePlot(day, averageEnergy);
            plots.get("Average lifetime").updatePlot(day, averageLifetime);
            plots.get("Average children number").updatePlot(day, averageChildrenNumber);
            genotype.setText("Dominant genotype: " + map.getTheMostFrequentGenotype().toString());
            if (ifMagicBorn) magicBorn.setText("Magic born " + magicBornCounter + "/3");
        });
    }
    public void clearGrid(GridPane grid){
        grid.getChildren().clear();
        grid.getRowConstraints().clear();
        grid.getColumnConstraints().clear();
    }
    public void prepareGrid(GridPane grid, AbstractMap map) {
        int height = map.getHeight();
        int width = map.getWidth();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Vector2d currPos = new Vector2d(x,y);
                Circle circle = getCircleForTheStrongestAnimal(currPos, map);
                Color backgroundColor = Color.LIGHTGREEN;

                if (map.isGrassOnPosition(new Vector2d(x,y))){
                    backgroundColor = Color.LIME;
                }
                else if (map.isPositionInJungle(currPos)){
                    backgroundColor = Color.GREEN;
                }

                HBox hbox = (circle == null) ? new HBox(): new HBox(circle);
                hbox.setAlignment(Pos.CENTER);

                hbox.setBackground(new Background(new BackgroundFill(backgroundColor, CornerRadii.EMPTY, Insets.EMPTY)));
                hbox.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1))));
                grid.add(hbox, x, y, 1, 1);
                GridPane.setHalignment(hbox, HPos.CENTER);

            }
        }
        for (int y = 0; y < height; y++) {
            grid.getRowConstraints().add(new RowConstraints(20));
        }
        for (int x = 0; x < width; x++) {
            grid.getColumnConstraints().add(new ColumnConstraints(30));
        }
    }

    public Circle getCircleForTheStrongestAnimal(Vector2d position, AbstractMap map) {
        List<Animal> positionAnimals = map.getSortedListOfAnimalsOnPosition(position);
        return (positionAnimals.size() != 0) ? getCircleForGivenAnimal(positionAnimals.get(0)) : null;
    }

    public Circle getCircleForGivenAnimal(Animal animal){
        Circle animalCircle = new Circle(6);
        animalCircle.setFill(animal.toColor(startEnergy));
        return animalCircle;
    }
}
