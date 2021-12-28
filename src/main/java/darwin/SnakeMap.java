package darwin;

public class SnakeMap extends AbstractMap{
    public SnakeMap(int width, int height, float jungleRatio) {
        super(width, height, jungleRatio);
        isFenced = false;
    }
}
