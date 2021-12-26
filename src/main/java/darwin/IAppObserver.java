package darwin;

public interface IAppObserver {
    void show(AbstractMap map, int day, int animalsNumber, int grassNumber, float averageEnergy,
              float averageLifetime, float averageChildrenNumber);
}
