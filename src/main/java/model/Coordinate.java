package model;

import util.Initializer;
import util.StreamUtil;

import java.util.ArrayList;
import java.util.List;

public class Coordinate {
    private int x;
    private int y;

    public Coordinate() {
    }

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Coordinate readFrom(java.io.InputStream stream) throws java.io.IOException {
        Coordinate result = new Coordinate();
        result.x = StreamUtil.readInt(stream);
        result.y = StreamUtil.readInt(stream);
        return result;
    }

    public List<Coordinate> getAdjacentList() {
        List<Coordinate> coordinateList = new ArrayList<>();
        if (x - 1 >= 0) {
            coordinateList.add(new Coordinate(x - 1, y));
        }
        if (y - 1 >= 0) {
            coordinateList.add(new Coordinate(x, y - 1));
        }
        if (y + 1 < Initializer.getMapSize()) {
            coordinateList.add(new Coordinate(x, y + 1));
        }
        if (x + 1 < Initializer.getMapSize()) {
            coordinateList.add(new Coordinate(x + 1, y));
        }
        return coordinateList;
    }

    public List<Coordinate> getAdjacentListWithSelf() {
        List<Coordinate> coordinateList = new ArrayList<>();
        coordinateList.add(this);
        if (x - 1 >= 0) {
            coordinateList.add(new Coordinate(x - 1, y));
        }
        if (y - 1 >= 0) {
            coordinateList.add(new Coordinate(x, y - 1));
        }
        if (y + 1 < Initializer.getMapSize()) {
            coordinateList.add(new Coordinate(x, y + 1));
        }
        if (x + 1 < Initializer.getMapSize()) {
            coordinateList.add(new Coordinate(x + 1, y));
        }
        return coordinateList;
    }

    public boolean isOutOfBounds() {
        return getX() < 0 || getY() < 0 || getX() >= Initializer.getMapSize() || getY() >= Initializer.getMapSize();
    }

    public boolean isInBounds() {
        return getX() >= 0 && getY() >= 0 && getX() < Initializer.getMapSize() && getY() < Initializer.getMapSize();
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
        StreamUtil.writeInt(stream, x);
        StreamUtil.writeInt(stream, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Coordinate that = (Coordinate) o;
        return x == that.x &&
                y == that.y;
    }

    @Override
    public int hashCode() {
        return x*80+y;
    }

    @Override
    public String toString() {
        return "Pos{" + x + "," + y + '}';
    }
}
