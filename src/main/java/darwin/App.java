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
import javafx.stage.Stage;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import static java.lang.String.valueOf;

public class App extends Application implements IAppObserver {
    SimulationEngine snakeEngine;
    SimulationEngine wallEngine;
    private boolean ifMagicBorn;
    private int startEnergy;
    private Scene menuScene;
    private DoublePlot snakeDoublePlot;
    private DoublePlot wallDoublePlot;
    private Thread engineSnakeThread;
    private Thread engineWallThread;
    private final GridPane snakeGrid = new GridPane();
    private final GridPane wallGrid = new GridPane();
    private final VBox wallAnimalObservedStats = new VBox(10);
    private final VBox snakeAnimalObservedStats = new VBox(10);
    private final Map<String, String> defaultMenuValues = createDefaultMenuValues();
    private final Map<String, TextField> menuTextFields = new HashMap<>();
    private final Label snakeGenotype = new Label();
    private final Label wallGenotype = new Label();
    private final Label snakeMagicBorn = new Label("Magic born 0/3");
    private final Label wallMagicBorn = new Label("Magic born 0/3");
    private final Map<String, Plot> wallPlots = new HashMap<>();
    private final Map<String, Plot> snakePlots = new HashMap<>();
    private final CSVHandler snakeCSVHandler = new CSVHandler("snake_map_stats");
    private final CSVHandler wallCSVHandler = new CSVHandler("wall_map_stats");
    private final int circleR = 5;

    public void start(Stage primaryStage) {
        Scene menuScene = new Scene(createVBoxMenu(primaryStage),400,600);
        this.menuScene = menuScene;

        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        primaryStage.setScene(menuScene);
        primaryStage.setTitle("Darwin World Simulation");
        primaryStage.show();
    }

    public void addPlots(Map<String, Plot> givenMap){
        givenMap.put("Average energy", new Plot("Average energy", startEnergy));
        givenMap.put("Average lifetime", new Plot("Average lifetime", 0));
        givenMap.put("Average children number", new Plot("Average children number", 0));
    }

    public VBox createVBoxMenu(Stage primaryStage){
        Button createSimulationButton = getButtonToCreateSimulation(primaryStage);
        Button exitButton = getExitButton();

        HBox buttonBox = new HBox(40, createSimulationButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);
        return new VBox(20,
                createVBoxWithLabelsAndTextFields("Map Properties",
                        new String[]{"Map height", "Map width", "Jungle ratio"}),
                createVBoxWithLabelsAndTextFields("Menu Elements Properties",
                        new String[]{"Grass energy", "Animal start energy", "Animal move energy"}),
                createVBoxWithLabelsAndTextFields("Others",
                        new String[]{"Start animals number", "Refresh time (in ms)", "Use magic born [yes/no]"}),
                buttonBox);
    }

    private Button getExitButton() {
        Button exitButton = new Button("Exit");
        exitButton.setOnAction(click -> {
            Platform.exit();
            System.exit(0);
        });

        return exitButton;
    }

    private Button getButtonToCreateSimulation(Stage primaryStage) {
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

        return createSimulationButton;
    }

    public Scene createSimulationScene(Map<String, Number> menuArgs, Stage primaryStage){
        prepareSimulation(menuArgs);
        prepareGrid(snakeGrid, snakeEngine);
        prepareGrid(wallGrid, wallEngine);

        engineSnakeThread = new Thread(snakeEngine);
        engineWallThread = new Thread(wallEngine);

        VBox snakeStatsVBox = getSnakeStatsVBox();
        VBox wallStatsVBox = getWallStatsVBox();

        HBox simulationsBox = new HBox(20, snakeStatsVBox, wallStatsVBox);
        VBox sceneBox = new VBox(20, createMapLegendHBox(), simulationsBox,
                getHBoxWithExitSimulationButton(primaryStage));
        simulationsBox.setAlignment(Pos.CENTER);
        sceneBox.setAlignment(Pos.CENTER);
        snakeGrid.setAlignment(Pos.CENTER);
        wallGrid.setAlignment(Pos.CENTER);
        return new Scene(sceneBox);
    }

    private VBox getWallStatsVBox() {
        return new VBox(20, prepareMapWithButtonsBox(wallGrid,
                prepareButtonsBox(wallEngine, wallCSVHandler, wallDoublePlot, wallPlots, wallGrid)),
                wallAnimalObservedStats,
                prepareStatsVBox(wallGenotype, wallMagicBorn, wallEngine.getMap(), wallDoublePlot, wallPlots));
    }

    private VBox getSnakeStatsVBox() {
        return new VBox(20, prepareMapWithButtonsBox(snakeGrid,
                prepareButtonsBox(snakeEngine, snakeCSVHandler, snakeDoublePlot, snakePlots, snakeGrid)),
                snakeAnimalObservedStats,
                prepareStatsVBox(snakeGenotype, snakeMagicBorn, snakeEngine.getMap(), snakeDoublePlot, snakePlots));
    }

    public HBox getHBoxWithExitSimulationButton(Stage primaryStage){
        Button exitButton = new Button("Exit to menu");
        exitButton.setOnAction(click -> {
            wallEngine.finished = true;
            snakeEngine.finished = true;
            primaryStage.setScene(menuScene);

            if (snakeEngine.getMap().observedAnimal != null){
                snakeEngine.getMap().observedAnimal.stopObserving();
                snakeEngine.getMap().observedAnimal = null;
                updateGuiForObserving(snakeGrid, snakeEngine, false);
            }

            if (wallEngine.getMap().observedAnimal != null){
                wallEngine.getMap().observedAnimal.stopObserving();
                wallEngine.getMap().observedAnimal = null;
                updateGuiForObserving(wallGrid, wallEngine, false);
            }
        });

        HBox hbox = new HBox(exitButton);
        hbox.setAlignment(Pos.CENTER);
        return hbox;
    }

    private void updateGuiForObserving(GridPane grid, SimulationEngine engine, boolean ifDominant) {
        clearGrid(grid);
        prepareGrid(grid, engine, ifDominant);
        prepareAnimalObservedStats(engine);
    }

    public HBox createMapLegendHBox(){
        Color[] rectanglesColors = {Color.LIGHTGREEN, Color.LIME, Color.GREEN};
        String[] rectanglesDescriptions = {"Step Field", "Grass Field", "Jungle Field"};
        Color[] circleColors = {Color.SADDLEBROWN, Color.RED, Color.BLUEVIOLET};
        String[] circleDescription = {"Animal (the darker the stronger)", "Animal with dominant genotype", "Observed animal"};
        HBox hBox = new HBox(20);

        for (int i=0; i < rectanglesColors.length; i++){
            HBox oneLegendField = new HBox(20, new Rectangle(10,10, rectanglesColors[i]), new Label(rectanglesDescriptions[i]));
            oneLegendField.setAlignment(Pos.CENTER);
            hBox.getChildren().add(oneLegendField);
        }

        for (int i=0; i < circleColors.length; i++){
            HBox oneLegendField = new HBox(20, new Circle(circleR, circleColors[i]), new Label(circleDescription[i]));
            oneLegendField.setAlignment(Pos.CENTER);
            hBox.getChildren().add(oneLegendField);
        }

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
        ifMagicBorn = (int) menuArgs.get("Use magic born [yes/no]") == 1;
        startEnergy = (int) menuArgs.get("Animal start energy");
        int delay = (int) menuArgs.get("Refresh time (in ms)");

        snakeEngine = new SimulationEngine(width, height, jungleRatio, startAnimalsNumber,
                startEnergy, moveEnergy, grassEnergy, delay, ifMagicBorn,false, this);
        wallEngine = new SimulationEngine(width, height, jungleRatio, startAnimalsNumber,
                startEnergy, moveEnergy, grassEnergy, delay, ifMagicBorn, true, this);

        snakeDoublePlot = new DoublePlot("Animals","Grasses", startAnimalsNumber, 0);
        wallDoublePlot = new DoublePlot("Animals","Grasses", startAnimalsNumber, 0);
        
        addPlots(wallPlots);
        addPlots(snakePlots);
    }

    public VBox createVBoxWithLabelsAndTextFields(String primaryLabel, String[] textFieldNames){
        Label label = getLabelWithBoldText(primaryLabel);
        VBox vbox = new VBox(10, label, createLabelsVBoxWithTextFields(textFieldNames));
        vbox.setAlignment(Pos.CENTER);
        return vbox;
    }

    public VBox createLabelsVBoxWithTextFields(String[] properties){
        VBox vBox = new VBox(30);
        vBox.setAlignment(Pos.CENTER);
        for (String oneProperty: properties){
            Label label = getLabelWithBoldText(oneProperty);

            TextField textField = new TextField(defaultMenuValues.get(oneProperty));
            textField.setAlignment(Pos.CENTER);

            menuTextFields.put(oneProperty, textField);
            HBox hbox = new HBox(10, label, textField);
            hbox.setAlignment(Pos.CENTER);
            vBox.getChildren().add(hbox);
        }

        return vBox;
    }

    public Map<String,String> createDefaultMenuValues(){
        Map<String,String> defaultMenuValues = new HashMap<>();
        defaultMenuValues.put("Map height", "10");
        defaultMenuValues.put("Map width", "10");
        defaultMenuValues.put("Jungle ratio", "0.4");
        defaultMenuValues.put("Grass energy", "30");
        defaultMenuValues.put("Animal start energy", "200");
        defaultMenuValues.put("Animal move energy", "5");
        defaultMenuValues.put("Use magic born [yes/no]", "no");
        defaultMenuValues.put("Start animals number", "15");
        defaultMenuValues.put("Refresh time (in ms)", "300");

        return defaultMenuValues;
    }

    public VBox prepareStatsVBox(Label givenGenotypeLabel, Label givenMagicBornLabel, AbstractMap map,
                                 DoublePlot doublePlot, Map<String, Plot> plots){
        HBox title = new HBox(10, getLabelWithBoldText("GENERAL STATS"));
        title.setAlignment(Pos.CENTER);

        givenGenotypeLabel.setText("Dominant genotype: " + map.getTheMostFrequentGenotype().toString());

        HBox magicBornAndGenotypeBox;
        if (ifMagicBorn)
            magicBornAndGenotypeBox = new HBox(10, givenGenotypeLabel, givenMagicBornLabel);
        else
            magicBornAndGenotypeBox = new HBox(givenGenotypeLabel);
        magicBornAndGenotypeBox.setAlignment(Pos.CENTER);

        HBox firstPlotsBox = preparePlotHBox(doublePlot, plots.get("Average energy"));
        HBox secondsPlotsBox = preparePlotHBox(plots.get("Average lifetime"), plots.get("Average children number"));

        VBox boxWithStatsTitle = new VBox(10, title, magicBornAndGenotypeBox, firstPlotsBox, secondsPlotsBox);
        boxWithStatsTitle.setAlignment(Pos.CENTER);

        return boxWithStatsTitle;
    }

    public HBox preparePlotHBox(DoublePlot doublePlot, Plot plot){
        HBox hbox = new HBox(10, doublePlot.getLineChart(), plot.getLineChart());
        hbox.setAlignment(Pos.CENTER);
        return hbox;
    }

    public HBox preparePlotHBox(Plot firstPlot, Plot secondPlot){
        HBox hbox = new HBox(10, firstPlot.getLineChart(), secondPlot.getLineChart());
        hbox.setAlignment(Pos.CENTER);
        return hbox;
    }

    public HBox prepareMapWithButtonsBox(GridPane mapGrid, VBox buttonsBox){
        HBox box = new HBox(10, mapGrid, buttonsBox);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    public VBox prepareButtonsBox(SimulationEngine givenSimulation, CSVHandler handler, DoublePlot doublePlot,
                                    Map<String, Plot> plots, GridPane grid){
        VBox box = new VBox(20, prepareStartButton(givenSimulation),
                prepareStopButton(givenSimulation),
                prepareToCSVButton(givenSimulation, handler, doublePlot, plots),
                prepareShowAnimalsWithGenotypeButton(givenSimulation, grid),
                prepareStopObservingButton(givenSimulation, grid));

        box.setAlignment(Pos.CENTER);
        return box;
    }

    public Button prepareStopObservingButton(SimulationEngine givenSimulation, GridPane grid){
        Button button = new Button("Stop Observing");
        button.setOnAction(click -> {
            if (!givenSimulation.ifRunning){
                if(givenSimulation.getMap().observedAnimal != null){
                    givenSimulation.getMap().observedAnimal.stopObserving();
                    givenSimulation.getMap().observedAnimal = null;
                }
                updateGuiForObserving(grid, givenSimulation, false);
            }
        });

        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return button;
    }

    public Button prepareShowAnimalsWithGenotypeButton(SimulationEngine givenSimulation, GridPane grid){
        Button button = new Button("Show dominant genotype animals");

        button.setOnAction(click -> {
            if (!givenSimulation.ifRunning){
                clearGrid(grid);
                prepareGrid(grid, givenSimulation, true);
            }
        });

        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return button;
    }

    public Button prepareToCSVButton(SimulationEngine givenSimulation, CSVHandler handler, DoublePlot doublePlot,
                                     Map<String, Plot> plots){
        Button button = new Button("Save to file");
        button.setOnAction(click -> {
            if (!givenSimulation.ifRunning){
                handler.updateData("Animals", doublePlot.getSeries1AllValues(), doublePlot.getSeries1Average());
                handler.updateData("Grass", doublePlot.getSeries2AllValues(), doublePlot.getSeries2Average());

                plots.forEach((plotName, plot) ->
                        handler.updateData(plotName, plot.getAllValues(), plot.getSeriesAverage()));

                try {
                    handler.createCSV();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return button;
    }

    public Button prepareStopButton(SimulationEngine givenSimulation){
        Button button = new Button("Stop Simulation");
        button.setOnAction(click -> givenSimulation.setIfRunning(false));

        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return button;
    }

    public Button prepareStartButton(SimulationEngine givenSimulation){
        Button button = new Button("Start Simulation");
        button.setOnAction(click -> givenSimulation.setIfRunning(true));

        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return button;
    }

    public void show(AbstractMap map, int day, int animalsNumber, int grassNumber, float averageEnergy,
                     float averageLifetime, float averageChildrenNumber, int magicBornCounter) {
        GridPane grid = wallGrid;
        DoublePlot doublePlot = wallDoublePlot;
        Map<String, Plot> plots = wallPlots;
        Label magicBorn = wallMagicBorn;
        Label genotype = wallGenotype;
        SimulationEngine engine = wallEngine;

        if (map instanceof SnakeMap) {
            grid = snakeGrid;
            doublePlot = snakeDoublePlot;
            plots = snakePlots;
            magicBorn = snakeMagicBorn;
            genotype = snakeGenotype;
            engine = snakeEngine;
        }

        updateGui(map, day, animalsNumber, grassNumber, averageEnergy, averageLifetime, averageChildrenNumber,
                magicBornCounter, grid, doublePlot, plots, magicBorn, genotype, engine);
    }

    private void updateGui(AbstractMap map, int day, int animalsNumber, int grassNumber, float averageEnergy,
                           float averageLifetime, float averageChildrenNumber, int magicBornCounter, GridPane grid,
                           DoublePlot doublePlot, Map<String, Plot> plots, Label magicBorn, Label genotype,
                           SimulationEngine engine) {
        Platform.runLater(() -> {
            clearGrid(grid);
            prepareGrid(grid, engine);
            doublePlot.updatePlot(day, animalsNumber, grassNumber);
            plots.get("Average energy").updatePlot(day, averageEnergy);
            plots.get("Average lifetime").updatePlot(day, averageLifetime);
            plots.get("Average children number").updatePlot(day, averageChildrenNumber);
            genotype.setText("Dominant genotype: " + map.getTheMostFrequentGenotype().toString());
            if (ifMagicBorn) magicBorn.setText("Magic born " + magicBornCounter + "/3");
            prepareAnimalObservedStats(engine);
        });
    }

    public void clearGrid(GridPane grid){
        grid.getChildren().clear();
        grid.getRowConstraints().clear();
        grid.getColumnConstraints().clear();
    }

    public void prepareGrid(GridPane grid, SimulationEngine engine, boolean ifDominant) {
        AbstractMap map = engine.getMap();
        int height = map.getHeight();
        int width = map.getWidth();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                updateOneCell(grid, engine, ifDominant, map, y, x);
            }
        }
        updateGridConstraints(grid, height, width);
    }

    private void updateGridConstraints(GridPane grid, int height, int width) {
        for (int y = 0; y < height; y++) {
            grid.getRowConstraints().add(new RowConstraints(10, 30, Double.MAX_VALUE));
        }
        for (int x = 0; x < width; x++) {
            grid.getColumnConstraints().add(new ColumnConstraints(10, 30, Double.MAX_VALUE));
        }
    }

    private void updateOneCell(GridPane grid, SimulationEngine engine, boolean ifDominant, AbstractMap map, int y, int x){
        Vector2d currPos = new Vector2d(x, y);
        Circle circle = getAnimalCircle(currPos, engine, ifDominant, grid);
        Color backgroundColor = Color.LIGHTGREEN;

        if (map.isGrassOnPosition(new Vector2d(x, y))){
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

    public void prepareGrid(GridPane grid, SimulationEngine engine){
        prepareGrid(grid, engine,false);
    }

    public void prepareAnimalObservedStats(SimulationEngine engine){
        AbstractMap map = engine.getMap();

        VBox statsBox;
        if (map instanceof WallMap) statsBox = wallAnimalObservedStats;
        else statsBox = snakeAnimalObservedStats;
        statsBox.getChildren().clear();

        if (map.observedAnimal == null) return;

        HBox statsLabelTitle = new HBox(10, getLabelWithBoldText("OBSERVING STATS"));
        statsLabelTitle.setAlignment(Pos.CENTER);

        HBox genotype = new HBox(10, getLabelWithBoldText("Genotype"), new Label(map.observedAnimal.getGenes().toString()));
        genotype.setAlignment(Pos.CENTER);

        HBox numberStatsBox = getNumObservedStatsHBox(map);

        statsBox.getChildren().addAll(statsLabelTitle, genotype, numberStatsBox);
        statsBox.setAlignment(Pos.CENTER);
    }

    private HBox getNumObservedStatsHBox(AbstractMap map) {
        HBox children = new HBox(10, getLabelWithBoldText("Children"),
                new Label(valueOf(map.observedAnimal.getChildrenNumberAfterObservingStarts())));
        HBox descendants = new HBox(10, getLabelWithBoldText("Descendants"),
                new Label(valueOf(map.observedAnimal.getAllDescendantsNumber())));
        HBox deadDate = new HBox(10, getLabelWithBoldText("Dead date"),
                new Label(map.observedAnimal.getDeadDateString()));

        HBox numberStatsBox = new HBox(10, children, descendants, deadDate);
        numberStatsBox.setAlignment(Pos.CENTER);
        return numberStatsBox;
    }

    public Label getLabelWithBoldText(String text){
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold");
        label.setAlignment(Pos.CENTER);
        return label;
    }

    public Circle getAnimalCircle(Vector2d position, SimulationEngine engine, boolean ifDominant, GridPane grid) {
        AbstractMap map = engine.getMap();
        Circle circle = getObservedCircle(map, position);
        Animal animal = (circle != null) ? map.observedAnimal : null;

        if (ifDominant && circle == null){
            ArrayList<Animal> animalsWithDominant = getAnimalsWithDominantGenotypeList(position, map);
            if (!animalsWithDominant.isEmpty()){
                circle = new Circle(circleR);
                animal = animalsWithDominant.get(animalsWithDominant.size()-1);
                circle.setFill(Color.RED);
            }
        }

        if (circle == null){
            List<Animal> positionAnimals = map.getSortedListOfAnimalsOnPositionDesc(position);
            circle = (positionAnimals.size() != 0) ? new Circle(circleR) : null;
            if (circle != null) {
                animal = positionAnimals.get(0);
                circle.setFill(animal.toColor(startEnergy));
            }
        }

        addClickEventToCircle(engine, ifDominant, grid, circle, animal);
        return circle;
    }

    private ArrayList<Animal> getAnimalsWithDominantGenotypeList(Vector2d position, AbstractMap map) {
        return map.getAnimalsFromGivenPosition(position).stream()
                .filter(a -> a.getGenes().equals(map.getTheMostFrequentGenotype()))
                .sorted((a1, a2) -> Float.compare(a1.getEnergy(), a2.getEnergy()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void addClickEventToCircle(SimulationEngine engine, boolean ifDominant, GridPane grid, Circle circle,
                                       Animal animal) {
        if (circle == null) return;
        circle.setOnMouseClicked(e -> {
            if (!engine.ifRunning){
                if (engine.getMap().observedAnimal != null){
                    engine.getMap().observedAnimal.stopObserving();
                }

                if (engine.getMap().observedAnimal == animal){
                    engine.getMap().observedAnimal = null;
                } else{
                    engine.getMap().observedAnimal = animal;
                    engine.getMap().observedAnimal.setObserved(true);
                }
                updateGuiForObserving(grid, engine, ifDominant);
            }
        });
    }

    public Circle getObservedCircle(AbstractMap map, Vector2d position){
        Circle circle = null;
        if (map.observedAnimal != null && map.observedAnimal.isAlive() && map.observedAnimal.getPosition().equals(position)){
            circle = new Circle(circleR);
            circle.setFill(Color.BLUEVIOLET);
        }
        return circle;
    }
}
