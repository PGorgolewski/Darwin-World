package darwin;

public class Grass extends AbstractMapElement{
    public Grass(Vector2d position, AbstractMap map, int grassEnergy, IObserver observer){
        this.position = position;
        this.map = map;
        this.energy = grassEnergy;
        this.observer = observer;
    }
}
