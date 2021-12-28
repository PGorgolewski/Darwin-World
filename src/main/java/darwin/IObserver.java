package darwin;

public interface IObserver {
    void positionChanged(AbstractMapElement element, Vector2d oldPosition);
}
