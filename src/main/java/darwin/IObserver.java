package darwin;

public interface IObserver {
    public void positionChanged(AbstractMapElement element, Vector2d oldPosition);
}
