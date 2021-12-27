package darwin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Animal extends AbstractMapElement {
    private MapDirections orient;
    private final List<Integer> genes = new ArrayList<>();
    private final int birthDay;
    private int lifetime = 0;
    private int childrenNumber = 0;
    private final List<Animal> children = new ArrayList<>();

    //INITIAL BORN
    public Animal(Vector2d startPosition, AbstractMap map, int startEnergy, IObserver observer, int birthDay) {
        this.position = startPosition;
        this.orient = getRandomOrient();
        this.map = map;
        getRandomGenotype();
        this.energy = startEnergy;
        this.observer = observer;
        this.birthDay = birthDay;
    }

    //NORMAL BORN
    public Animal(Animal dad, Animal mom, AbstractMap map, IObserver observer, int birthDay) {
        this.position = dad.getPosition();
        this.orient = getRandomOrient();
        this.map = map;
        getGenotypeFromParents(dad, mom);
        this.energy = getEnergyFromParents(dad, mom);
        this.observer = observer;
        this.birthDay = birthDay;
        dad.childrenNumber++;
        mom.childrenNumber++;
    }

    //MAGIC BORN
    public Animal(Vector2d startPosition, AbstractMap map, Animal animalToCopy, int startEnergy, IObserver observer, int birthDay){
        this.position = startPosition;
        this.orient = animalToCopy.getOrient();
        this.map = map;
        this.genes.addAll(animalToCopy.getGenes());
        this.energy = startEnergy;
        this.observer = observer;
        this.birthDay = birthDay;
    }

    public void move(int moveEnergy) {
        int directionNumber = this.genes.get(ThreadLocalRandom.current().nextInt(0, 32));

        if (directionNumber == 0) moveForward();
        else if (directionNumber == 4) moveBackward();
        else this.orient = this.orient.getDirectionAfterRotation(directionNumber);

        this.energy -= moveEnergy;
        this.lifetime++;
    }

    public void moveForward() {
        Vector2d oldPosition = new Vector2d(this.position);
        if (this.map instanceof WallMap) {
            this.setPosition(this.validatePositionForWall(this.position.add(this.orient.toUnitVector())));
        } else {
            this.setPosition(this.validatePositionForSnake(this.position.add(this.orient.toUnitVector())));
        }
        this.observer.positionChanged(this, oldPosition);
    }

    public void moveBackward(){
        Vector2d oldPosition = new Vector2d(this.position);
        if (this.map instanceof WallMap) {
            this.setPosition(this.validatePositionForWall(this.position.subtract(this.orient.toUnitVector())));
        } else {
            this.setPosition(this.validatePositionForSnake(this.position.subtract(this.orient.toUnitVector())));
        }
        this.observer.positionChanged(this, oldPosition);
    }

    public Vector2d validatePositionForWall(Vector2d potentialVector){
        int x = potentialVector.getX();
        int y = potentialVector.getY();

        if (x < 0) x = 0;
        else if (x >= this.map.getWidth()) x = this.map.getWidth() - 1;

        if (y < 0) y = 0;
        else if (y >= this.map.getHeight()) y = this.map.getHeight() - 1;

        return new Vector2d(x, y);
    }

    public Vector2d validatePositionForSnake(Vector2d potentialVector){
        int x = potentialVector.getX();
        int y = potentialVector.getY();

        x %= this.map.getWidth();
        y %= this.map.getHeight();

        //resolving problem of (-1)%10=(-1) not 9
        if (x<0) x+=this.map.getWidth();
        if (y<0) y+=this.map.getHeight();

        return new Vector2d(x, y);
    }

    private MapDirections getRandomOrient(){
        int randomNum = ThreadLocalRandom.current().nextInt(0, 7 + 1);

        return MapDirections.getMapDirectionFromDirectionNumber(randomNum);
    }

    private float getEnergyFromParents(Animal dad, Animal mom){
        float energyFromDad = dad.getEnergy() / 4;
        float energyFromMom = mom.getEnergy() / 4;

        dad.setEnergy(dad.getEnergy()-energyFromDad);
        mom.setEnergy(mom.getEnergy()-energyFromMom);

        return energyFromDad + energyFromMom;
    }

    private void getRandomGenotype(){
        Random random = new Random();
        List<Integer> occurrences = Stream.generate(() -> 0).limit(8).collect(Collectors.toList());

        random.ints(32, 0,8)
                .forEach(index -> occurrences.set(index, occurrences.get(index)+1));

        IntStream.range(0, occurrences.size()).forEach(index -> {
            List<Integer> oneGenList = Stream.generate(() -> index).limit(occurrences.get(index))
                    .collect(Collectors.toList());
            this.genes.addAll(oneGenList);
        });
    }

    private void getGenotypeFromParents(Animal dad, Animal mom){
        boolean ifDadShouldBeFirst = new Random().nextBoolean();

        int numberOfDadGens = Math.round(32 * (dad.getEnergy() / (dad.getEnergy() + mom.getEnergy())));

        if (ifDadShouldBeFirst) {
            this.genes.addAll(dad.getGenes().subList(0,numberOfDadGens));
            this.genes.addAll(mom.getGenes().subList(numberOfDadGens,32));
        } else {
            this.genes.addAll(mom.getGenes().subList(0,32-numberOfDadGens));
            this.genes.addAll(dad.getGenes().subList(32-numberOfDadGens,32));
        }
    }

    public List<Integer> getGenes() {
        return genes;
    }

    public MapDirections getOrient() {
        return orient;
    }

    public int getLifetime() {
        return lifetime;
    }

    public int getChildrenNumber() {
        return childrenNumber;
    }

    public void setChildrenNumber(int childrenNumber) {
        this.childrenNumber = childrenNumber;
    }
}
