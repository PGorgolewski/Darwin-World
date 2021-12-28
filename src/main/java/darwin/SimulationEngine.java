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
    private final int moveDelay;
    private final AbstractMap map;
    private int currAnimalsNumber;
    private final IAppObserver observer;
    private int magicBornCounter = 0;
    boolean ifRunning = false;
    boolean finished = false;
    private int currDay = 0;
    private int deadAnimalsCounter = 0;
    private float averageLifetime = 0;
    private final Set<Animal> allAnimals = new HashSet<>();
    private final Set<Animal> deadAnimals = new HashSet<>();
    private final Set<Vector2d> positionsWithAnimalAndGrass = new HashSet<>();

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
        while(allAnimals.size() > 0 && !finished){
            waitForStartButton();
            deleteDeadAnimals();
            moveEachAnimal();
            eatGrasses();
            animalReproduction();
            map.grassGrowing(grassEnergy);
            updateMap();
        }
    }

    public void waitForStartButton(){
        synchronized (this){
            while (!ifRunning){
                try {
                    wait();
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void magicBorn(){
        List<Animal> parents = allAnimals.stream().
                filter(a -> !deadAnimals.contains(a))
                .collect(Collectors.toCollection(ArrayList::new));

        if (parents.size() != 5) return;

        magicBornCounter++;
        List<Vector2d> positionsWithoutAnimals = map.getPositionsWithoutAnimals();
        magicReproduction(positionsWithoutAnimals, parents);
    }

    protected void magicReproduction(List<Vector2d> freePositions, List<Animal> parents){
        for (Animal parent: parents){
            int randomNumber = ThreadLocalRandom.current().nextInt(0, freePositions.size());
            Animal magicBabyAnimal = new Animal(freePositions.get(randomNumber), map, parent, startEnergy,
                    map, currDay);
            allAnimals.add(magicBabyAnimal);
            map.placeElement(magicBabyAnimal);
            currAnimalsNumber++;
            freePositions.remove(randomNumber);
            }
        }

    public void animalReproduction(){
        Map<Vector2d, List<Animal>> positionsByAnimals = map.getPositionsByAnimalsMap(2,
                minReproductionEnergy);

        positionsByAnimals.forEach((k, v) -> {
            List<Animal> aliveAnimals = v.stream()
                    .filter(e -> !deadAnimals.contains(e))
                    .sorted((a1, a2) -> Float.compare(a1.getEnergy(), a2.getEnergy()))
                    .collect(Collectors.toCollection(ArrayList::new));

            if (aliveAnimals.size() >= 2){
                givingBirth(aliveAnimals);
            }
        });

        if (ifMagicBorn && magicBornCounter < 3) magicBorn();
    }

    private void givingBirth(List<Animal> aliveAnimals) {
        Collections.reverse(aliveAnimals);
        Animal dad = aliveAnimals.get(0);
        Animal mom = aliveAnimals.get(1);
        Animal child = new Animal(dad, mom, map, map, currDay);

        allAnimals.add(child);
        map.placeElement(child);
        currAnimalsNumber++;

        dad.setChildrenNumber(dad.getChildrenNumber()+1);
        mom.setChildrenNumber(mom.getChildrenNumber()+1);

        addToChildrenListIfParentObserved(dad, child);
        addToChildrenListIfParentObserved(mom, child);
    }

    public void addToChildrenListIfParentObserved(Animal parent, Animal child){
        if (parent.isObserved){
            parent.addChildWhenObserved(child);
            child.setObserved(true);
        }
    }

    public void eatGrasses(){
        positionsWithAnimalAndGrass.forEach(position -> {
            List<Animal> animals = map.getSortedListOfAnimalsOnPositionDesc(position);
            Grass grass = map.grassMap.get(position);

            List<Animal> theStrongestAnimalsList = getTheStrongestAnimals(animals);
            if (theStrongestAnimalsList != null){
                float energyPerAnimal = Objects.requireNonNull(grass).getEnergy() / theStrongestAnimalsList.size();
                theStrongestAnimalsList.forEach(animal -> animal.setEnergy(animal.getEnergy() + energyPerAnimal));
                map.removeElement(grass, position);
            }
        });

        positionsWithAnimalAndGrass.clear();
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
        for (Animal currentAnimal: allAnimals){
            if (ifAnimalCanMove(currentAnimal)){
                Vector2d beforeMovePosition = currentAnimal.getPosition();
                currentAnimal.move(moveEnergy);
                Vector2d afterMovePosition = currentAnimal.getPosition();
                if (beforeMovePosition != afterMovePosition && map.isGrassOnPosition(afterMovePosition))
                    positionsWithAnimalAndGrass.add(afterMovePosition);
            }else{
                currentAnimal.setAsDead();
                deadAnimals.add(currentAnimal);
            }
        }
    }

    public void deleteDeadAnimals(){
        for (Animal deadAnimal: new HashSet<>(deadAnimals)){
            deadAnimalsCounter++;
            updateAverageLifetime(deadAnimal);
            map.removeElement(deadAnimal, deadAnimal.getPosition());
            allAnimals.remove(deadAnimal);
        }

        currAnimalsNumber -= deadAnimals.size();
        deadAnimals.clear();
    }

    public void updateAverageLifetime(Animal deadAnimal){
        averageLifetime = averageLifetime * ((float) (deadAnimalsCounter-1)/deadAnimalsCounter) +
                (float) deadAnimal.getLifetime()/deadAnimalsCounter;
    }

    public void createFirstAnimals(int startingAnimalsNumber, int startEnergy){
        Set<Vector2d> allFreePositionsSet = new HashSet<>(map.jungleFreePositions);
        allFreePositionsSet.addAll(map.stepFreePositions);
        List<Vector2d> allFreePositionsList = new ArrayList<>(allFreePositionsSet);
        
        for (int i = 0; i < startingAnimalsNumber; i++){
            int randomNum = ThreadLocalRandom.current().nextInt(0, allFreePositionsList.size());
            Animal newAnimal = new Animal(new Vector2d(allFreePositionsList.get(randomNum)), map,
                    startEnergy, map, currDay);
            
            map.placeElement(newAnimal);
            allAnimals.add(newAnimal);

            allFreePositionsList.remove(randomNum);
        }
    }

    public float getAverageEnergy(){
        float sumOfEnergy = 0;
        int counter = 0;
        for (Animal animal: allAnimals) {
            if (deadAnimals.contains(animal)) continue;
            sumOfEnergy += animal.getEnergy();
            counter++;
        }
        
        return sumOfEnergy / counter;
    }

    public float getAverageChildrenNumber(){
        int sumOfChildren = 0;
        int counter = 0;
        for (Animal animal: allAnimals) {
            if (deadAnimals.contains(animal)) continue;
            sumOfChildren += animal.getChildrenNumber();
            counter++;
        }
        return (float) sumOfChildren / counter;
    }

    public void updateMap(){
        currDay++;
        int currentGrassAmount = map.grassMap.size();

        observer.show(map, currDay, currAnimalsNumber, currentGrassAmount, getAverageEnergy(),
                averageLifetime, getAverageChildrenNumber(), magicBornCounter);

        try {
            Thread.sleep(moveDelay);
        } catch (InterruptedException e) {
            System.out.println("The simulation has stopped");
        }
    }

    public void setIfRunning(boolean ifRunning) {
        synchronized (this){
            this.ifRunning = ifRunning;
            notifyAll();
        }
    }

    public boolean ifAnimalCanMove(Animal animal){
        return animal.getEnergy() >= moveEnergy;
    }

    public AbstractMap getMap() {
        return map;
    }
}
