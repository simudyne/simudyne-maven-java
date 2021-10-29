package org.example.models.schelling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Grid {

    public Cell[][] cells;

    public List<Cell> cellList;

    public Random random;

    public final int gridSize;

    public Grid(int gridSize, int seed) {
        this.gridSize = gridSize;

        random = new Random(seed);

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
        Collections.shuffle(cellList, random);
        for (int i = 0; i < nbBlue + nbRed; ++i)
            cellList.get(i).setState(i < nbBlue ? CellState.BLUE : CellState.RED);
    }

}
