package org.example.models.schelling;

import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class Environment extends GlobalState {
    @Input(name = "Grid Size")
    public int gridSize = 30;

    @Input(name = "Empty Cells Proportion")
    public double emptyCellsPcg = 0.1;

    @Input(name = "Similarity Threshold")
    public double similarityThreshold = 0.33;

    public int nbTotal;

    public int nbOccupied;

    public int nbEmpty;

    public int nbBlue;

    public int nbRed;

    private HashMap<Long, AgentState> agentMap;

    public Grid grid;

    public Environment() {
        this.initTotals();
        grid = new Grid(gridSize);
        agentMap = new HashMap<>();

        grid.init(nbBlue, nbRed);
    }

    public void initTotals() {
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

    public void register(long agentID, AgentState agentState) {
        agentMap.put(agentID, agentState);
    }


    public void initPositions() {
        agentMap.forEach((agentID, agentState) -> {
            Optional<Cell> optionalCell = grid.cellList.stream().filter(x -> x.sameAgentState(agentState.race) && !x.occupied).findFirst();

            if (!optionalCell.isPresent())
                throw new IllegalArgumentException("No position found.");

            Cell cell = optionalCell.get();
            cell.occupied = true;
            agentState.position = cell;
        });
    }

    public double calculateSimilarityMetric(Coordinates coordinates, AgentState.AgentRace agentRace) {
        int neighbours = 0;
        int similarNeighbours = 0;
        double similarityMetric = 0.0;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                Coordinates newCoordinates = new Coordinates(coordinates.x + i, coordinates.y + j);
                if (newCoordinates.x >= 0 && newCoordinates.y < gridSize &&
                        newCoordinates.y >= 0 && newCoordinates.x < gridSize && !(i == 0 && j == 0)) {
                    if (grid.cells[newCoordinates.x][newCoordinates.y].state != CellState.EMPTY) {
                        neighbours += 1;
                        if (grid.cells[newCoordinates.x][newCoordinates.y].sameAgentState(agentRace)) {
                            similarNeighbours += 1;
                        }
                    }
                }
            }
        }
        if (neighbours != 0) {
            similarityMetric = (double) similarNeighbours / neighbours;
        }
        return similarityMetric;
    }

    public void moveAgent(AgentState agentState) {
        Optional<Cell> optionalCell = grid.cellList.stream().filter(x ->
                !x.occupied && calculateSimilarityMetric(x.coordinates, agentState.race) >=
                        similarityThreshold).findAny();

        if (!optionalCell.isPresent())
            return;

        Cell cell = optionalCell.get();
        cell.occupied = true;
        cell.switchState(agentState.race);
        agentState.changePosition(cell);
    }

    public AgentState getAgentState(long agentID) {
        return agentMap.get(agentID);
    }

    public void calculateSimilarityMetrics() {
        agentMap.forEach((agentID, agentState) -> {
            double similarityMetric = calculateSimilarityMetric(agentState.position.coordinates, agentState.race);
            agentState.changeSimilarityMetric(similarityMetric);
        });
    }

}