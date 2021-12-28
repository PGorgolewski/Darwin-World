package darwin;

public class WallMap extends AbstractMap{
    public WallMap(int width, int height, float jungleRatio) {
        super(width, height, jungleRatio);
        isFenced = true;
    }
}
