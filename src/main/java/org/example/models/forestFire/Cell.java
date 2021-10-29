package org.example.models.forestFire;

public class Cell {
    public int xCoordinate;
    public int yCoordinate;
    public boolean empty;

    public Cell(int xCoordinate, int yCoordinate, boolean empty) {
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.empty = empty;
    }

    public int getXCoordinate() {
        return xCoordinate;
    }

    public int getYCoordinate() {
        return yCoordinate;
    }

    public boolean isEmpty() {
        return empty;
    }
}
