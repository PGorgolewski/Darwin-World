package darwin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

abstract class AbstractMap implements IObserver{
    protected final int width;
    protected final int height;
    protected final Vector2d jungleLowerLeft;
    protected final Vector2d jungleUpperRight;
    protected final Set<Vector2d> jungleFreePositions = new HashSet<>();
    protected final Set<Vector2d> stepFreePositions = new HashSet<>();
    protected final Map<Vector2d, Set<AbstractMapElement>> jungleElementPositions = new HashMap<>();
    protected final Map<Vector2d, Set<AbstractMapElement>> stepElementPositions = new HashMap<>();

    protected AbstractMap(int width, int height, float jungleRatio) {
        this.width = width;
        this.height = height;
        int jungleWidth = Math.round(this.width * jungleRatio);
        int jungleHeight = Math.round(this.height * jungleRatio);
        this.jungleLowerLeft = new Vector2d((this.width - jungleWidth) / 2, (this.height - jungleHeight) / 2);
        this.jungleUpperRight = new Vector2d(this.jungleLowerLeft.getX() + jungleWidth - 1,
                this.jungleLowerLeft.getY() + jungleHeight - 1);

        this.initialAddingAllPositionsAsFree();
    }

    protected void initialAddingAllPositionsAsFree(){
        for (int x = 0; x < this.width; x++){
            for (int y = 0; y < this.height; y++){
                Vector2d currVector = new Vector2d(x,y);
                if (isPositionInJungle(currVector)){
                    this.jungleFreePositions.add(currVector);
                    this.jungleElementPositions.put(currVector, new HashSet<>());
                }else {
                    this.stepFreePositions.add(currVector);
                    this.stepElementPositions.put(currVector, new HashSet<>());
                }
            }
        }
    }

    protected void grassGrowing(int grassEnergy){
        grassGrowingForGivenArea(this.jungleFreePositions, grassEnergy);
        grassGrowingForGivenArea(this.stepFreePositions, grassEnergy);
    }

    protected void grassGrowingForGivenArea(Set<Vector2d> freePositions, int grassEnergy){
        if (freePositions.size() > 0){
            int randomNumber = ThreadLocalRandom.current().nextInt(0, freePositions.size());

            int currentIndex = 0;
            Vector2d randomPosition = null;
            for (Vector2d position : freePositions) {
                if (currentIndex == randomNumber){
                    randomPosition = position;
                    break;
                }
                currentIndex++;
            }

            this.placeElement(new Grass(randomPosition, this, grassEnergy, this));
        }
    }

    protected void placeElement(AbstractMapElement mapElement){
        Vector2d elementVector = mapElement.getPosition();
        if (isPositionInJungle(elementVector)){
            this.placeElementForGivenMap(mapElement, elementVector, this.jungleElementPositions, this.jungleFreePositions);
        }else {
            this.placeElementForGivenMap(mapElement, elementVector, this.stepElementPositions, this.stepFreePositions);
        }
    }

    protected void placeElementForGivenMap(AbstractMapElement mapElement, Vector2d elementVector,
                                           Map<Vector2d, Set<AbstractMapElement>> givenMap, Set<Vector2d> givenSet){
        givenSet.remove(elementVector);
        givenMap.get(elementVector).add(mapElement);
    }

    protected void removeElement(AbstractMapElement mapElement, Vector2d elementPosition){
        if (isPositionInJungle(elementPosition)){
            this.removeGivenElement(mapElement, elementPosition, this.jungleElementPositions, this.jungleFreePositions);
        }else {
            this.removeGivenElement(mapElement, elementPosition, this.stepElementPositions, this.stepFreePositions);
        }
    }

    protected void removeGivenElement(AbstractMapElement mapElement, Vector2d elementPosition,
                                      Map<Vector2d, Set<AbstractMapElement>> givenElementPositions,
                                      Set<Vector2d> givenFreePositions){
        givenElementPositions.get(elementPosition).remove(mapElement);
        if (givenElementPositions.get(elementPosition).size() == 0){
            givenFreePositions.add(elementPosition);
        }
    }

    protected boolean isPositionInJungle(Vector2d position){
        return this.jungleLowerLeft.precedes(position) && this.jungleUpperRight.follows(position);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public Set<AbstractMapElement> getElementsFromGivenPosition(Vector2d position) {
        if (isPositionInJungle(position)){
            return this.jungleElementPositions.get(position);
        }else {
            return this.stepElementPositions.get(position);
        }
    }

    public List<Animal> getSortedListOfAnimalsOnPosition(Vector2d position){
        return this.getElementsFromGivenPosition(position).stream()
                .filter(e -> e instanceof Animal)
                .map(e -> (Animal) e)
                .sorted((a1, a2) -> Float.compare(a1.getEnergy(), a2.getEnergy()))
                .collect(Collectors.toCollection(ArrayList::new));
    }
    protected Map<Vector2d, List<Animal>> getPositionsByAnimalsMap(int minAnimals, float minEnergy){
        Map<Vector2d, List<Animal>> positionsByAnimals = getPositionsByAnimalFromGivenMap(minAnimals, minEnergy,
                this.jungleElementPositions);
        positionsByAnimals.putAll(getPositionsByAnimalFromGivenMap(minAnimals, minEnergy, this.stepElementPositions));
        return positionsByAnimals;
    }

    protected Map<Vector2d, List<Animal>> getPositionsByAnimalFromGivenMap(int minAnimals, float minEnergy,
                                                                           Map<Vector2d, Set<AbstractMapElement>> givenMap){
        Map<Vector2d, List<Animal>> positionsByAnimals = new HashMap<>();
        givenMap.forEach((k,v) -> {
            List<Animal> animalsOnPosition = v.stream()
                    .filter(e -> (e instanceof Animal && e.getEnergy() >= minEnergy))
                    .map(e -> (Animal) e)
                    .collect(Collectors.toCollection(ArrayList::new));

            if (animalsOnPosition.size() >= minAnimals){
                positionsByAnimals.put(k,animalsOnPosition);
            }
        });

        return positionsByAnimals;
    }

    public Vector2d getJungleLowerLeft() {
        return jungleLowerLeft;
    }

    public Vector2d getJungleUpperRight() {
        return jungleUpperRight;
    }

    public void positionChanged(AbstractMapElement element, Vector2d oldPosition){
        this.removeElement(element, oldPosition);
        if (element instanceof Animal){
            this.placeElement(element);
        }
    }

    protected boolean isGrassOnPosition(Vector2d position){
        if (isPositionInJungle(position)){
            return this.isGrassOnPositionForGivenMap(this.jungleElementPositions.get(position));
        }else {
            return this.isGrassOnPositionForGivenMap(this.stepElementPositions.get(position));
        }
    }

    protected boolean isGrassOnPositionForGivenMap(Set<AbstractMapElement> setOfElements){
        return setOfElements.stream().anyMatch(element -> element instanceof Grass);
    }
}
