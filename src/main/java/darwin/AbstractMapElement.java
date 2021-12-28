package darwin;

import javafx.scene.paint.Color;

abstract class AbstractMapElement {
    protected Vector2d position;
    protected AbstractMap map;
    protected float energy;
    protected IObserver observer;

    public Color toColor(int startEnergy) {
        if (energy == 0) return Color.rgb(222, 221, 224);
        if (energy < 0.2 * startEnergy) return Color.rgb(224, 179, 173);
        if (energy < 0.4 * startEnergy) return Color.rgb(224, 142, 127);
        if (energy < 0.6 * startEnergy) return Color.rgb(201, 124, 110);
        if (energy < 0.8 * startEnergy) return Color.rgb(182, 105, 91);
        if (energy < startEnergy) return Color.rgb(164, 92, 82);
        if (energy < 2 * startEnergy) return Color.rgb(146, 82, 73);
        if (energy < 4 * startEnergy) return Color.rgb(128, 72, 64);
        if (energy < 6 * startEnergy) return Color.rgb(119, 67, 59);
        if (energy < 8 * startEnergy) return Color.rgb(88, 50, 44);
        if (energy < 10 * startEnergy) return Color.rgb(74, 42, 37);
        return Color.rgb(55, 31, 27);
    }

    public Vector2d getPosition(){
        return position;
    }

    public float getEnergy() {
        return energy;
    }

    public void setPosition(Vector2d position) {
        this.position = position;
    }

    public void setEnergy(float energy) {
        this.energy = energy;
    }
}
