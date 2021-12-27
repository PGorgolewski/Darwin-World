package darwin;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO zatrzymywanie i wznawianie
//TODO autoresizing and legend for map
//TODO zrobParser

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
    private DoublePlot snakeDoublePlot;
    private DoublePlot wallDoublePlot;
    private final Label snakeGenotype = new Label();
    private final Label wallGenotype = new Label();
    private final Label snakeMagicBorns = new Label("Magic borns 0/3");
    private final Label wallMagicBorns = new Label("Magic borns 0/3");
    private final Map<String, Plot> wallPlots = new HashMap<>();
    private final Map<String, Plot> snakePlots = new HashMap<>();

    public void init() {
        int width = 10;
        int height = 10;
        float jungleRatio = 0.4f;
        int startAnimalsNumber = 20;
        int startEnergy = 200;
        int grassEnergy = 40;
        int moveEnergy = 5;
        this.ifMagicBorn = false;
        this.startEnergy = startEnergy;
        this.snakeEngine = new SimulationEngine(width, height, jungleRatio, startAnimalsNumber,
                startEnergy, moveEnergy, grassEnergy, ifMagicBorn,false, this);
        this.wallEngine = new SimulationEngine(width, height, jungleRatio, startAnimalsNumber,
                startEnergy, moveEnergy, grassEnergy, ifMagicBorn, true, this);
        this.snakeDoublePlot = new DoublePlot("Animals","Grasses", startAnimalsNumber, 0);
        this.wallDoublePlot = new DoublePlot("Animals","Grasses", startAnimalsNumber, 0);
        this.addPlots(this.wallPlots);
        this.addPlots(this.snakePlots);
    }

    public void addPlots(Map<String, Plot> givenMap){
        givenMap.put("Average energy", new Plot("Average energy", startEnergy));
        givenMap.put("Average lifetime", new Plot("Average lifetime", 0));
        givenMap.put("Average children number", new Plot("Average children number", 0));
    }

    public void start(Stage primaryStage) {
        prepareGrid(snakeGrid, snakeEngine.getMap());
        prepareGrid(wallGrid, wallEngine.getMap());

        Thread engineSnakeThread = new Thread(snakeEngine);
        Thread engineWallThread = new Thread(wallEngine);

        HBox startStopSnakeBox = prepareStartStopBox(engineSnakeThread);
        HBox startStopWallBox = prepareStartStopBox(engineWallThread);

        VBox snakeVBox = new VBox(40, snakeGrid, startStopSnakeBox,
                prepareGenotypeDominantAndMagicBornInfo(snakeGenotype, snakeMagicBorns, snakeEngine.getMap()),
                this.prepareAllPlotsVBox(snakeDoublePlot, snakePlots));
        VBox wallVBox = new VBox(40, wallGrid, startStopWallBox,
                prepareGenotypeDominantAndMagicBornInfo(wallGenotype, wallMagicBorns, wallEngine.getMap()),
                this.prepareAllPlotsVBox(wallDoublePlot, wallPlots));

        HBox hBox = new HBox(40, snakeVBox, wallVBox);
        hBox.setAlignment(Pos.CENTER);
        snakeGrid.setAlignment(Pos.CENTER);
        wallGrid.setAlignment(Pos.CENTER);
        Scene scene = new Scene(hBox ,800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Darwin World Simulation");
        primaryStage.show();
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

    public HBox prepareStartStopBox(Thread givenSimulationThread){
        HBox box = new HBox(20, prepareStartButton(givenSimulationThread),
                prepareStopButton(givenSimulationThread));
        box.setAlignment(Pos.CENTER);
        return box;
    }
    public Button prepareStopButton(Thread givenSimulationThread){
        Button button = new Button("Stop Simulation");

        button.setOnAction(click -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        return button;
    }

    public Button prepareStartButton(Thread givenSimulationThread){
        Button button = new Button("Start Simulation");

        button.setOnAction(click -> {
            givenSimulationThread.start();
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
            magicBorn = wallMagicBorns;
            genotype = wallGenotype;
        }
        else {
            grid = snakeGrid;
            doublePlot = snakeDoublePlot;
            plots = snakePlots;
            magicBorn = snakeMagicBorns;
            genotype = snakeGenotype;
        }

        Platform.runLater(() -> {
            grid.getChildren().clear();
            grid.getRowConstraints().clear();
            grid.getColumnConstraints().clear();
            grid.setGridLinesVisible(false);
            prepareGrid(grid, map);
            doublePlot.updatePlot(day,animalsNumber,grassNumber);
            plots.get("Average energy").updatePlot(day, averageEnergy);
            plots.get("Average lifetime").updatePlot(day, averageLifetime);
            plots.get("Average children number").updatePlot(day, averageChildrenNumber);
            genotype.setText("Dominant genotype: " + map.getTheMostFrequentGenotype().toString());
            if (ifMagicBorn) magicBorn.setText("Magic borns " + magicBornCounter + "/3");
        });
    }

    public void prepareGrid(GridPane grid, AbstractMap map) {
        grid.setGridLinesVisible(true);
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
