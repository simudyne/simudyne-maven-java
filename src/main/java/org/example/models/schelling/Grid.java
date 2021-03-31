package org.example.models.schelling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Grid {
    public Cell[][] cells;

    public List<Cell> cellList;

    private final int gridSize;

    public Grid(int gridSize) {
        this.gridSize = gridSize;

        cells = new Cell[gridSize][gridSize];

        cellList = new ArrayList<>();

        for (int i = 0; i < gridSize; ++i) {
            for (int j = 0; j < gridSize; ++j) {
                cells[i][j] = new Cell(i, j);
                cellList.add(cells[i][j]);
            }
        }
    }

    public void init(int nbBlue, int nbRed) {
        // todo: make this deterministic
        Collections.shuffle(cellList);
        for (int i = 0; i < nbBlue + nbRed; ++i)
            cellList.get(i).setState(i < nbBlue ? CellState.BLUE : CellState.RED);
    }
}
