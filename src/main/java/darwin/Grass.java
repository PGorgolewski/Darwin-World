package darwin;

import javafx.scene.paint.Color;

public class Grass extends AbstractMapElement{
    public Grass(Vector2d position, AbstractMap map, int grassEnergy, IObserver observer){
        this.position = position;
        this.map = map;
        this.energy = grassEnergy;
        this.observer = observer;
    }

    @Override
    public Color toColor(int startEnergy) {
        return Color.LIMEGREEN;
    }
}
