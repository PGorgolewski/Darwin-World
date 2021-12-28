package darwin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

abstract class AbstractMap implements IObserver{
    protected final int width;
    protected final int height;
    protected final Vector2d jungleLowerLeft;
    protected final Vector2d jungleUpperRight;
    protected boolean isFenced;
    protected final Set<Vector2d> jungleFreePositions = new HashSet<>();
    protected final Set<Vector2d> stepFreePositions = new HashSet<>();
    protected final Map<Vector2d, Grass> grassMap = new HashMap<>();
    protected final Map<Vector2d, Set<Animal>> animalMap = new HashMap<>();
    protected final Map<List<Integer>, Integer> genotypeOccurrences = new HashMap<>();
    protected Animal observedAnimal = null;


    protected AbstractMap(int width, int height, float jungleRatio) {
        this.width = width;
        this.height = height;
        int jungleWidth = Math.round(width * jungleRatio);
        int jungleHeight = Math.round(height * jungleRatio);
        jungleLowerLeft = new Vector2d((width - jungleWidth) / 2, (height - jungleHeight) / 2);
        jungleUpperRight = new Vector2d(jungleLowerLeft.getX() + jungleWidth - 1,
                jungleLowerLeft.getY() + jungleHeight - 1);

        initialAddingAllPositionsAsFree();
    }

    protected void initialAddingAllPositionsAsFree(){
        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                Vector2d currVector = new Vector2d(x, y);

                if (isPositionInJungle(currVector))
                    jungleFreePositions.add(currVector);
                else
                    stepFreePositions.add(currVector);

                animalMap.put(currVector, new HashSet<>());
            }
        }
    }

    protected void grassGrowing(int grassEnergy){
        grassGrowingForGivenArea(jungleFreePositions, grassEnergy);
        grassGrowingForGivenArea(stepFreePositions, grassEnergy);
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

            placeElement(new Grass(randomPosition, this, grassEnergy, this));
        }
    }

    protected void addToGenotypeMap(List<Integer> genes){
        if (genotypeOccurrences.containsKey(genes))
            genotypeOccurrences.replace(genes, genotypeOccurrences.get(genes) + 1);
        else
            genotypeOccurrences.put(genes, 1);
    }

    protected void removeFromGenotypeMap(List<Integer> genes){
        genotypeOccurrences.replace(genes, genotypeOccurrences.get(genes) - 1);
        if (genotypeOccurrences.get(genes) == 0)
            genotypeOccurrences.remove(genes);
    }

    protected void placeElement(AbstractMapElement mapElement){
        Vector2d elementVector = mapElement.getPosition();

        if (isPositionInJungle(elementVector))
            placeElementForGivenMap(mapElement, elementVector, jungleFreePositions);
        else
            placeElementForGivenMap(mapElement, elementVector,  stepFreePositions);

        if (mapElement instanceof Animal) addToGenotypeMap(((Animal) mapElement).getGenes());
    }

    protected void placeElementForGivenMap(AbstractMapElement mapElement, Vector2d elementVector,
                                           Set<Vector2d> givenFreePositionsSet){
        givenFreePositionsSet.remove(elementVector);

        if (mapElement instanceof Animal)
            animalMap.get(elementVector).add((Animal) mapElement);
        else
            grassMap.put(elementVector, (Grass) mapElement);
    }

    protected void removeElement(AbstractMapElement mapElement, Vector2d elementPosition){
        if (isPositionInJungle(elementPosition))
            removeGivenElement(mapElement, elementPosition, jungleFreePositions);
        else
            removeGivenElement(mapElement, elementPosition, stepFreePositions);


        if (mapElement instanceof Animal) removeFromGenotypeMap(((Animal) mapElement).getGenes());
    }

    protected void removeGivenElement(AbstractMapElement mapElement, Vector2d elementPosition,
                                      Set<Vector2d> givenFreePositions){
        if (mapElement instanceof Animal)
            animalMap.get(elementPosition).remove(mapElement);
        else
            grassMap.remove(elementPosition);


        if (animalMap.get(elementPosition).size() == 0 && !grassMap.containsKey(elementPosition))
            givenFreePositions.add(elementPosition);

    }

    protected boolean isPositionInJungle(Vector2d position){
        return jungleLowerLeft.precedes(position) && jungleUpperRight.follows(position);
    }

    public Set<Animal> getAnimalsFromGivenPosition(Vector2d position){
        return animalMap.get(position);
    }

    public List<Animal> getSortedListOfAnimalsOnPosition(Vector2d position){
        return getAnimalsFromGivenPosition(position).stream()
                .sorted((a1, a2) -> Float.compare(a1.getEnergy(), a2.getEnergy()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<Animal> getSortedListOfAnimalsOnPositionDesc(Vector2d position){
        List<Animal> animalsList = getSortedListOfAnimalsOnPosition(position);
        Collections.reverse(animalsList);
        return animalsList;
    }

    protected Map<Vector2d, List<Animal>> getPositionsByAnimalsMap(int minAnimals, float minEnergy){
        Map<Vector2d, List<Animal>> positionsByAnimals = new HashMap<>();

        animalMap.forEach((k,v) -> {
            List<Animal> animalsOnPosition = v.stream()
                    .filter(e -> (e.getEnergy() >= minEnergy))
                    .collect(Collectors.toCollection(ArrayList::new));

            if (animalsOnPosition.size() >= minAnimals)
                positionsByAnimals.put(k,animalsOnPosition);

        });

        return positionsByAnimals;
    }

    protected List<Vector2d> getPositionsWithoutAnimals(){
        List<Vector2d> positionsWithoutAnimals = new ArrayList<>();

        animalMap.forEach((k,v) -> {
            if (v.size() == 0){
                positionsWithoutAnimals.add(k);
            }
        });

        return positionsWithoutAnimals;
    }

    protected List<Integer> getTheMostFrequentGenotype(){
        List<Integer> theMostFrequent = new ArrayList<>();
        int maxOccurrences = 0;

        for (List<Integer> genotype: genotypeOccurrences.keySet()){
            if (genotypeOccurrences.get(genotype) > maxOccurrences){
                maxOccurrences = genotypeOccurrences.get(genotype);
                theMostFrequent = genotype;
            }
        }

        return theMostFrequent;
    }

    public void positionChanged(AbstractMapElement element, Vector2d oldPosition){
        removeElement(element, oldPosition);
        if (element instanceof Animal)
            placeElement(element);
    }

    protected boolean isGrassOnPosition(Vector2d position){
        return grassMap.containsKey(position);
    }

    public int getHeight(){
        return height;
    }

    public int getWidth(){
        return width;
    }
}
