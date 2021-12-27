package darwin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

abstract class AbstractMap implements IObserver{
    protected final int width;
    protected final int height;
    protected final Vector2d jungleLowerLeft;
    protected final Vector2d jungleUpperRight;
    protected final Set<Vector2d> jungleFreePositions = new HashSet<>();
    protected final Set<Vector2d> stepFreePositions = new HashSet<>();
    protected final Map<Vector2d, Grass> grassMap = new HashMap<>();
    protected final Map<Vector2d, Set<Animal>> animalMap = new HashMap<>();
    protected final Map<List<Integer>, Integer> genotypeOccurences = new HashMap<>();

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
                }else {
                    this.stepFreePositions.add(currVector);
                }
                this.animalMap.put(currVector, new HashSet<>());
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

    protected void addToGenotypeMap(List<Integer> genes){
        if (this.genotypeOccurences.containsKey(genes))
            this.genotypeOccurences.replace(genes, this.genotypeOccurences.get(genes) + 1);
        else
            this.genotypeOccurences.put(genes, 1);
    }

    protected void removeFromGenotypeMap(List<Integer> genes){
        this.genotypeOccurences.replace(genes, this.genotypeOccurences.get(genes) - 1);
        if (this.genotypeOccurences.get(genes) == 0)
            this.genotypeOccurences.remove(genes);
    }

    protected void placeElement(AbstractMapElement mapElement){
        Vector2d elementVector = mapElement.getPosition();
        if (isPositionInJungle(elementVector)){
            this.placeElementForGivenMap(mapElement, elementVector, this.jungleFreePositions);
        }else {
            this.placeElementForGivenMap(mapElement, elementVector,  this.stepFreePositions);
        }
        if (mapElement instanceof Animal) this.addToGenotypeMap(((Animal) mapElement).getGenes());
    }

    protected void placeElementForGivenMap(AbstractMapElement mapElement, Vector2d elementVector, Set<Vector2d> givenSet){
        givenSet.remove(elementVector);
        if (mapElement instanceof Animal){
            this.animalMap.get(elementVector).add((Animal) mapElement);
        }else{
            this.grassMap.put(elementVector, (Grass) mapElement);
        }
    }

    protected void removeElement(AbstractMapElement mapElement, Vector2d elementPosition){
        if (isPositionInJungle(elementPosition)){
            this.removeGivenElement(mapElement, elementPosition, this.jungleFreePositions);
        }else {
            this.removeGivenElement(mapElement, elementPosition, this.stepFreePositions);
        }
        if (mapElement instanceof Animal) this.removeFromGenotypeMap(((Animal) mapElement).getGenes());
    }

    protected void removeGivenElement(AbstractMapElement mapElement, Vector2d elementPosition,
                                      Set<Vector2d> givenFreePositions){
        if (mapElement instanceof Animal){
            this.animalMap.get(elementPosition).remove(mapElement);
        }else {
            this.grassMap.remove(elementPosition);
        }

        if (this.animalMap.get(elementPosition).size() == 0 && !this.grassMap.containsKey(elementPosition)){
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

    public Set<Animal> getAnimalsFromGivenPosition(Vector2d position) {
        return this.animalMap.get(position);
    }

    public List<Animal> getSortedListOfAnimalsOnPosition(Vector2d position){
        return this.getAnimalsFromGivenPosition(position).stream()
                .sorted((a1, a2) -> Float.compare(a1.getEnergy(), a2.getEnergy()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    protected Map<Vector2d, List<Animal>> getPositionsByAnimalsMap(int minAnimals, float minEnergy){
        Map<Vector2d, List<Animal>> positionsByAnimals = new HashMap<>();
        this.animalMap.forEach((k,v) -> {
            List<Animal> animalsOnPosition = v.stream()
                    .filter(e -> (e.getEnergy() >= minEnergy))
                    .collect(Collectors.toCollection(ArrayList::new));

            if (animalsOnPosition.size() >= minAnimals){
                positionsByAnimals.put(k,animalsOnPosition);
            }
        });
        return positionsByAnimals;
    }

    protected List<Vector2d> getPositionsWithoutAnimals(){
        List<Vector2d> positionsWithoutAnimals = new ArrayList<>();
        this.animalMap.forEach((k,v) -> {
            if (v.size() == 0){
                positionsWithoutAnimals.add(k);
            }
        });
        return positionsWithoutAnimals;
    }

    protected List<Integer> getTheMostFrequentGenotype(){
        List<Integer> theMostFrequent = new ArrayList<>();
        int maxOccurrences = 0;

        for (List<Integer> genotype: this.genotypeOccurences.keySet()){
            if (genotypeOccurences.get(genotype) > maxOccurrences){
                maxOccurrences = genotypeOccurences.get(genotype);
                theMostFrequent = genotype;
            }
        }

        return theMostFrequent;
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
        return this.grassMap.containsKey(position);
    }
}
