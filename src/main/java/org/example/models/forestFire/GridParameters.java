package org.example.models.forestFire;

import java.util.ArrayList;
import java.util.Collections;

public class GridParameters {
    public int nbTotal;
    public int nbOccupied;
    public int nbEmpty;
    public ArrayList<Integer> yCoordinates = new ArrayList<Integer>();
    public ArrayList<Integer> xCoordinates = new ArrayList<Integer>();
    public ArrayList<Boolean> stateList = new ArrayList<>();
    public ArrayList<Cell> cellList = new ArrayList<Cell>();

    public GridParameters(int gridSize, double emptyCellsPcg) {
        nbTotal = (int) Math.pow(gridSize, 2);
        nbEmpty = (int) Math.ceil(nbTotal * emptyCellsPcg);
        nbOccupied = nbTotal - nbEmpty;
        int counter = 0;

        for (int i = 0; i < gridSize; ++i) {
            for (int j = 0; j < gridSize; ++j) {

                xCoordinates.add(i);
                yCoordinates.add(j);
                if(counter < nbOccupied) {
                    stateList.add(false);
                } else {
                    stateList.add(true);
                }
                counter++;
            }
        }

        Collections.shuffle(stateList);
        for (int k =0; k < nbTotal; k++){
                Cell newCell= new Cell((int) xCoordinates.get(k), (int) yCoordinates.get(k), stateList.get(k));
                cellList.add(newCell);
        }
    }

    public Cell assignCell(){
        Cell outputCell = cellList.get(0);
        cellList.remove(0);
        return outputCell;
    }
}
