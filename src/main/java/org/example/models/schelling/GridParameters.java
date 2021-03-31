package org.example.models.schelling;

public class GridParameters {
    public int nbTotal;

    public int nbOccupied;

    public int nbEmpty;

    public int nbBlue;

    public int nbRed;

    public GridParameters(int gridSize, double emptyCellsPcg) {
        nbTotal = (int) Math.pow(gridSize, 2);
        nbEmpty = (int) Math.ceil(nbTotal * emptyCellsPcg);
        nbOccupied = nbTotal - nbEmpty;

        if (nbOccupied % 2 != 0) {
            nbEmpty += 1;
            nbOccupied -= 1;
        }
        nbBlue = nbOccupied / 2;
        nbRed = nbOccupied - nbBlue;
    }
}
