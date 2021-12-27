package darwin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class SimulationEngine implements Runnable{
    private final int moveEnergy;
    private final int grassEnergy;
    private final float minReproductionEnergy;
    private final int startEnergy;
    private final boolean ifMagicBorn;
    private int magicBornCounter = 0;
    private final AbstractMap map;
    boolean ifRunning = false;
    boolean finished = false;
    private final Set<Animal> allAnimals = new HashSet<>();
    private final Set<Animal> deadAnimals = new HashSet<>();
    private final Set<Vector2d> positionsWithAnimalAndGrass = new HashSet<>();
    private final IAppObserver observer;
    private int moveDelay;
    private int currDay=0;
    private int currAnimalsNumber;
    private int deadAnimalsCounter=0;
    private float averageLifetime = 0;

    public SimulationEngine(int mapWidth, int mapHeight, float jungleRatio, int startingAnimalsNumber, int startEnergy,
                            int moveEnergy, int grassEnergy, int moveDelay, boolean ifMagicBorn, boolean ifWallMap,
                            IAppObserver observer){
        if (ifWallMap)  this.map = new WallMap(mapWidth, mapHeight, jungleRatio);
        else this.map = new SnakeMap(mapWidth, mapHeight, jungleRatio);
        this.moveEnergy = moveEnergy;
        this.grassEnergy = grassEnergy;
        this.startEnergy = startEnergy;
        this.minReproductionEnergy = (float) startEnergy / 2;
        this.observer = observer;
        this.createFirstAnimals(startingAnimalsNumber, startEnergy);
        this.moveDelay = moveDelay;
        this.currAnimalsNumber = startingAnimalsNumber;
        this.ifMagicBorn = ifMagicBorn;
    }

    @Override
    public void run() {
        int counter = 0;
        while(allAnimals.size() > 0 && !finished){
            System.out.println(counter++);
            this.waitForStartButton();
            this.deleteDeadAnimals();
            this.moveEachAnimal();
            this.eatGrasses();
            this.animalReproduction();
            this.map.grassGrowing(this.grassEnergy);
            this.updateMap();
        }
    }

    public void waitForStartButton(){
        synchronized (this){
            while (!ifRunning){
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void magicBorn(){
        List<Animal> parents = this.allAnimals.stream().
                filter(a -> !deadAnimals.contains(a)).
                collect(Collectors.toCollection(ArrayList::new));

        if (parents.size() != 5) return;
        magicBornCounter++;
        List<Vector2d> positionsWithoutAnimals = this.map.getPositionsWithoutAnimals();
        magicReproduction(positionsWithoutAnimals, parents);
    }

    protected void magicReproduction(List<Vector2d> freePositions, List<Animal> parents){
        for (Animal parent: parents){
            int randomNumber = ThreadLocalRandom.current().nextInt(0, freePositions.size());
            Animal magicBabyAnimal = new Animal(freePositions.get(randomNumber), this.map, parent, this.startEnergy,
                    this.map, this.currDay);
            this.allAnimals.add(magicBabyAnimal);
            this.map.placeElement(magicBabyAnimal);
            this.currAnimalsNumber++;
            freePositions.remove(randomNumber);
            }

        }

    public void animalReproduction(){
        if (ifMagicBorn && magicBornCounter < 3) magicBorn();
        Map<Vector2d, List<Animal>> positionsByAnimals = this.map.getPositionsByAnimalsMap(2,
                this.minReproductionEnergy);

        positionsByAnimals.forEach((k, v) -> {
            List<Animal> aliveAnimals = v.stream()
                    .filter(e -> !this.deadAnimals.contains(e))
                    .sorted((a1, a2) -> Float.compare(a1.getEnergy(), a2.getEnergy()))
                    .collect(Collectors.toCollection(ArrayList::new));

            if (aliveAnimals.size() >= 2){
                Animal babyAnimal = new Animal(aliveAnimals.get(0), aliveAnimals.get(1), this.map, this.map, this.currDay);
                aliveAnimals.get(0).setChildrenNumber(aliveAnimals.get(0).getChildrenNumber()+1);
                aliveAnimals.get(1).setChildrenNumber(aliveAnimals.get(1).getChildrenNumber()+1);
                allAnimals.add(babyAnimal);
                this.map.placeElement(babyAnimal);
                this.currAnimalsNumber++;
            }
        });
    }

    public void eatGrasses(){
        this.positionsWithAnimalAndGrass.forEach(position -> {
            List<Animal> animals = this.map.getSortedListOfAnimalsOnPosition(position);
            Grass grass = this.map.grassMap.get(position);

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
            this.deadAnimalsCounter++;
            this.updateLifetime(deadAnimal);
            this.map.removeElement(deadAnimal, deadAnimal.getPosition());
            this.allAnimals.remove(deadAnimal);
        }
        this.currAnimalsNumber -= this.deadAnimals.size();
        this.deadAnimals.clear();
    }

    public void updateLifetime(Animal deadAnimal){
        this.averageLifetime = this.averageLifetime * ((float) (this.deadAnimalsCounter-1)/this.deadAnimalsCounter) +
                (float) deadAnimal.getLifetime()/this.deadAnimalsCounter;
    }

    public void createFirstAnimals(int startingAnimalsNumber, int startEnergy){
        Set<Vector2d> allFreePositionsSet = new HashSet<>(this.map.jungleFreePositions);
        allFreePositionsSet.addAll(this.map.stepFreePositions);
        System.out.println(allFreePositionsSet);
        for (int i = 0; i < startingAnimalsNumber; i++){
            List<Vector2d> allFreePositionsList = new ArrayList<>(allFreePositionsSet);
            System.out.println(allFreePositionsList.size());
            int randomNum = ThreadLocalRandom.current().nextInt(0, allFreePositionsList.size());
            Animal newAnimal = new Animal(new Vector2d(allFreePositionsList.get(randomNum)), this.map, startEnergy, this.map, this.currDay);
            this.map.placeElement(newAnimal);
            allFreePositionsSet.remove(allFreePositionsList.get(randomNum));

            this.allAnimals.add(newAnimal);
        }
    }

    public float getAverageEnergy(){
        float sumOfEnergy = 0;
        int counter = 0;
        for (Animal animal: this.allAnimals) {
            if (this.deadAnimals.contains(animal)) continue;
            sumOfEnergy += animal.getEnergy();
            counter++;
        }
        return sumOfEnergy / counter;
    }

    public float getAverageChildrenNumber(){
        int sumOfChildren = 0;
        int counter = 0;
        for (Animal animal: this.allAnimals) {
            if (this.deadAnimals.contains(animal)) continue;
            sumOfChildren += animal.getChildrenNumber();
            counter++;
        }
        return (float) sumOfChildren / counter;
    }

    public void updateMap(){
        this.currDay++;
        int currGrassNumber = this.map.grassMap.size();

        this.observer.show(this.map, this.currDay, this.currAnimalsNumber, currGrassNumber, getAverageEnergy(),
                this.averageLifetime, getAverageChildrenNumber(), this.magicBornCounter);

        try {
            Thread.sleep(this.moveDelay);
        } catch (InterruptedException e) {
            System.out.println("The simulation has stopped");
        }
    }

    public AbstractMap getMap() {
        return map;
    }

    public void setIfRunning(boolean ifRunning) {
        synchronized (this){
            this.ifRunning = ifRunning;
            notifyAll();
        }

    }
}
