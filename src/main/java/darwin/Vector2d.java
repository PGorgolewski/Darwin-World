package darwin;

import java.util.Objects;

public class Vector2d {
    final public int x;
    final public int y;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Vector2d(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vector2d(Vector2d vector) {
        this.x = vector.getX();
        this.y = vector.getY();
    }

    public boolean precedes(Vector2d other){
        return this.x <= other.x && this.y <= other.y;
    }

    public boolean follows(Vector2d other){
        return this.x >= other.x && this.y >= other.y;
    }

    public Vector2d upperRight(Vector2d other){
        int greaterX = this.x;
        int greaterY = this.y;

        if (other.x > greaterX)
            greaterX = other.x;
        if (other.y > greaterY)
            greaterY = other.y;

        return new Vector2d(greaterX, greaterY);
    }

    public Vector2d lowerLeft(Vector2d other){
        int lowerX = this.x;
        int lowerY = this.y;

        if (other.x < lowerX)
            lowerX = other.x;
        if (other.y < lowerY)
            lowerY = other.y;

        return new Vector2d(lowerX, lowerY);
    }

    public Vector2d add(Vector2d other){
        int newX = this.x + other.x;
        int newY = this.y + other.y;
        return new Vector2d(newX, newY);
    }

    public Vector2d subtract(Vector2d other){
        int newX = this.x - other.x;
        int newY = this.y - other.y;
        return new Vector2d(newX, newY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector2d)) return false;
        Vector2d vector2d = (Vector2d) o;
        return x == vector2d.x && y == vector2d.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public Vector2d opposite(){
        return new Vector2d(x * (-1), y * (-1));
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
