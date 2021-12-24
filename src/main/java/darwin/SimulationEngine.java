package darwin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class SimulationEngine implements Runnable{
    private final int maxEnergy;
    private final int moveEnergy;
    private final int grassEnergy;
    private final float minReproductionEnergy;
    private final AbstractMap map;
    private final Set<Animal> allAnimals = new HashSet<>();
    private final Set<Animal> deadAnimals = new HashSet<>();
    private final Set<Vector2d> positionsWithAnimalAndGrass = new HashSet<>();
    private final IAppObserver observer;
    private final int moveDelay = 300;
    private final int startEnergy;

    public SimulationEngine(int mapWidth, int mapHeight, float jungleRatio, int startingAnimalsNumber, int startEnergy,
                            int maxEnergy, int moveEnergy, int grassEnergy, Boolean ifWallMap, IAppObserver observer){
        if (ifWallMap)  this.map = new WallMap(mapWidth, mapHeight, jungleRatio);
        else this.map = new SnakeMap(mapWidth, mapHeight, jungleRatio);
        this.maxEnergy = maxEnergy;
        this.moveEnergy = moveEnergy;
        this.grassEnergy = grassEnergy;
        this.minReproductionEnergy = (float) startEnergy / 2;
        this.observer = observer;
        this.createFirstAnimalsPositions(startingAnimalsNumber, startEnergy);
        this.startEnergy = startEnergy;
    }

    @Override
    public void run() {
        while(true){
            this.deleteDeadAnimals();
            this.moveEachAnimal();
            this.eatGrasses();
            this.animalReproduction();
            this.map.grassGrowing(this.grassEnergy);
            this.updateMap();
        }
    }

    public void animalReproduction(){
        Map<Vector2d, List<Animal>> positionsByAnimals = this.map.getPositionsByAnimalsMap(2,
                this.minReproductionEnergy);

        positionsByAnimals.forEach((k, v) -> {
            List<Animal> aliveAnimals = v.stream()
                    .filter(e -> !this.deadAnimals.contains(e))
                    .sorted((a1, a2) -> Float.compare(a1.getEnergy(), a2.getEnergy()))
                    .collect(Collectors.toCollection(ArrayList::new));

            if (aliveAnimals.size() >= 2){
                Animal babyAnimal = new Animal(aliveAnimals.get(0), aliveAnimals.get(1), this.map, this.map);
                allAnimals.add(babyAnimal);
                this.map.placeElement(babyAnimal);
            }
        });
    }

    public void eatGrasses(){
        this.positionsWithAnimalAndGrass.forEach(position -> {
            Set<AbstractMapElement> elementsOnPosition = this.map.getElementsFromGivenPosition(position);
            Grass grass = null;
            List<Animal> animals = new ArrayList<>();
            for (AbstractMapElement element: elementsOnPosition){
                if (element instanceof Animal){
                    animals.add((Animal) element);
                }else {
                    grass = (Grass) element;
                }
            }
            List<Animal> theStrongestAnimalsList = this.getTheStrongestAnimals(animals);
            if (theStrongestAnimalsList != null){
                float energyPerAnimal = Objects.requireNonNull(grass).getEnergy() / theStrongestAnimalsList.size();
                theStrongestAnimalsList.forEach(animal -> animal.setEnergy(animal.getEnergy() + energyPerAnimal));
                this.map.removeElement(grass, position);
            }
        });
        this.positionsWithAnimalAndGrass.clear();
    }

    public List<Animal> getTheStrongestAnimals(List<Animal> animals){
        if (animals.size() == 0) return null;
        animals.sort((a1, a2) -> Float.compare(a1.getEnergy(), a2.getEnergy()));
        List<Animal> theStrongestAnimals = new ArrayList<>(List.of(animals.get(0)));
        int index = 1;
        while(index < animals.size() && animals.get(index) == animals.get(0)){
            theStrongestAnimals.add(animals.get(index));
            index++;
        }

        return theStrongestAnimals;
    }

    public void moveEachAnimal(){
        for (Animal currentAnimal: this.allAnimals){
            if (this.ifAnimalCanMove(currentAnimal)){
                Vector2d beforeMovePosition = currentAnimal.getPosition();
                currentAnimal.move(this.moveEnergy);
                Vector2d afterMovePosition = currentAnimal.getPosition();
                if (beforeMovePosition != afterMovePosition && this.map.isGrassOnPosition(afterMovePosition)){
                    this.positionsWithAnimalAndGrass.add(afterMovePosition);
                }
            }else{
                this.deadAnimals.add(currentAnimal);
            }
        }
    }

    public boolean ifAnimalCanMove(Animal animal){
        return animal.getEnergy() >= this.moveEnergy;
    }

    public void deleteDeadAnimals(){
        for (Animal deadAnimal: new HashSet<>(this.deadAnimals)){
            this.map.removeElement(deadAnimal, deadAnimal.getPosition());
            this.allAnimals.remove(deadAnimal);
        }

        this.deadAnimals.clear();
    }

    public void createFirstAnimalsPositions(int startingAnimalsNumber, int startEnergy){
        Set<Vector2d> allFreePositionsSet = new HashSet<>(this.map.jungleFreePositions);
        allFreePositionsSet.addAll(this.map.stepFreePositions);

        for (int i = 0; i < startingAnimalsNumber; i++){
            List<Vector2d> allFreePositionsList = new ArrayList<>(allFreePositionsSet);
            int randomNum = ThreadLocalRandom.current().nextInt(0, allFreePositionsList.size());
            Animal newAnimal = new Animal(new Vector2d(allFreePositionsList.get(randomNum)), this.map, startEnergy, this.map);
            this.map.placeElement(newAnimal);
            allFreePositionsSet.remove(allFreePositionsList.get(randomNum));

            this.allAnimals.add(newAnimal);
        }
    }

    public void updateMap(){
        this.observer.show(this.map);

        try {
            Thread.sleep(this.moveDelay);
        } catch (InterruptedException e) {
            System.out.println("The simulation has stopped");
        }
    }

    public AbstractMap getMap() {
        return map;
    }
}
