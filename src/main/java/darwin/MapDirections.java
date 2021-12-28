package darwin;

public enum MapDirections {
    NORTH(0),
    NORTHEAST(1),
    EAST(2),
    SOUTHEAST(3),
    SOUTH(4),
    SOUTHWEST(5),
    WEST(6),
    NORTHWEST(7);

    final int directionNumber;

    MapDirections(int directionNumber){
        this.directionNumber = directionNumber;
    }

    public MapDirections getDirectionAfterRotation(int rotation){
        int afterRotationNumber = (directionNumber + rotation) % 8;

        return getMapDirectionFromDirectionNumber(afterRotationNumber);
    }

    public static MapDirections getMapDirectionFromDirectionNumber(int givenNumber) {
        for(MapDirections direction: MapDirections.values()) {
            if (direction.directionNumber == givenNumber) {
                return direction;
            }
        }

        return null;
    }

    public Vector2d toUnitVector(){
        int x = 0;
        int y = 0;

        switch (this){
            case NORTH -> y = 1;
            case NORTHEAST -> {
                x = 1;
                y = 1;
            }
            case EAST -> x = 1;
            case SOUTHEAST -> {
                x = 1;
                y = -1;
            }
            case SOUTH -> y = -1;
            case SOUTHWEST -> {
                x = -1;
                y = -1;
            }
            case WEST -> x = -1;
            case NORTHWEST -> {
                x = -1;
                y = 1;
            }
        }

        return new Vector2d(x, y);
    }
}
