package darwin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.valueOf;

public class Animal extends AbstractMapElement {
    private MapDirections orient;
    private final int birthDay;
    private int lifetime = 0;
    private boolean isAlive = true;
    private int childrenNumber = 0;
    public boolean isObserved = false;
    private final List<Integer> genes = new ArrayList<>();
    private final List<Animal> childrenAfterObservingStarts = new ArrayList<>();

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
    public Animal(Vector2d startPosition, AbstractMap map, Animal animalToCopy,
                  int startEnergy, IObserver observer, int birthDay){
        this.position = startPosition;
        this.orient = animalToCopy.getOrient();
        this.map = map;
        this.genes.addAll(animalToCopy.getGenes());
        this.energy = startEnergy;
        this.observer = observer;
        this.birthDay = birthDay;
    }

    public void move(int moveEnergy) {
        int directionNumber = genes.get(ThreadLocalRandom.current().nextInt(0, 32));

        if (directionNumber == 0) moveForward();
        else if (directionNumber == 4) moveBackward();
        else orient = orient.getDirectionAfterRotation(directionNumber);

        energy -= moveEnergy;
        lifetime++;
    }

    public void moveForward() {
        Vector2d oldPosition = new Vector2d(position);

        if (map.isFenced)
            setPosition(validatePositionWhenFenced(position.add(orient.toUnitVector())));
        else
            setPosition(validatePositionWhenNotFenced(position.add(orient.toUnitVector())));

        observer.positionChanged(this, oldPosition);
    }

    public void moveBackward(){
        Vector2d oldPosition = new Vector2d(position);

        if (map.isFenced)
            setPosition(validatePositionWhenFenced(position.subtract(orient.toUnitVector())));
        else
            setPosition(validatePositionWhenNotFenced(position.subtract(orient.toUnitVector())));

        observer.positionChanged(this, oldPosition);
    }

    public Vector2d validatePositionWhenFenced(Vector2d potentialVector){
        int x = potentialVector.getX();
        int y = potentialVector.getY();

        if (x < 0) x = 0;
        else if (x >= map.getWidth()) x = map.getWidth() - 1;

        if (y < 0) y = 0;
        else if (y >= map.getHeight()) y = map.getHeight() - 1;

        return new Vector2d(x, y);
    }

    public Vector2d validatePositionWhenNotFenced(Vector2d potentialVector){
        int x = potentialVector.getX();
        int y = potentialVector.getY();

        x %= map.getWidth();
        y %= map.getHeight();

        //resolving problem of (-1)%10=(-1) not 9
        if (x<0) x+=map.getWidth();
        if (y<0) y+=map.getHeight();

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
            List<Integer> oneGenList = Stream.generate(() -> index)
                    .limit(occurrences.get(index))
                    .collect(Collectors.toList());

            genes.addAll(oneGenList);
        });
    }

    private void getGenotypeFromParents(Animal dad, Animal mom){
        boolean dadGenesOnTheLeft = new Random().nextBoolean();

        int numberOfDadGens = Math.round(32 * (dad.getEnergy() / (dad.getEnergy() + mom.getEnergy())));

        if (dadGenesOnTheLeft) {
            genes.addAll(dad.getGenes().subList(0,numberOfDadGens));
            genes.addAll(mom.getGenes().subList(numberOfDadGens,32));
        }else {
            genes.addAll(mom.getGenes().subList(0,32-numberOfDadGens));
            genes.addAll(dad.getGenes().subList(32-numberOfDadGens,32));
        }
    }

    public void stopObserving(){
        for (Animal child: childrenAfterObservingStarts){
            child.stopObserving();
        }
        childrenAfterObservingStarts.clear();
        setObserved(false);
    }

    public void addChildWhenObserved(Animal child){
        childrenAfterObservingStarts.add(child);
    }

    public Set<Animal> createSetOfAllDescendants(){
        Set<Animal> descendants = new HashSet<>(childrenAfterObservingStarts);

        for (Animal child: childrenAfterObservingStarts){
            descendants.addAll(child.createSetOfAllDescendants());
        }

        return descendants;
    }

    public int getAllDescendantsNumber(){
        return createSetOfAllDescendants().size();
    }

    public int getChildrenNumberAfterObservingStarts(){
        return childrenAfterObservingStarts.size();
    }

    public String getDeadDateString(){
        if (isAlive)
            return "is alive";

        return valueOf(birthDay+lifetime);
    }

    public void setChildrenNumber(int childrenNumber) {
        this.childrenNumber = childrenNumber;
    }

    public void setObserved(boolean observed) {
        isObserved = observed;
    }

    public void setAsDead(){
        isAlive = false;
    }

    public boolean isAlive() {
        return isAlive;
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
}
