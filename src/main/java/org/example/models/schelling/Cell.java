package org.example.models.schelling;

public class Cell {
    public Coordinates coordinates;

    public CellState state;

    public boolean occupied;

    public Cell(int i, int j, CellState state) {
        coordinates = new Coordinates(i, j);
        this.state = state;
    }

    public Cell(int i, int j) {
        this(i, j, CellState.EMPTY);
    }

    public void setState(CellState state) {
        this.state = state;
    }

    public void switchState(AgentState.AgentRace race) {
        if (race == AgentState.AgentRace.BLUE)
            setState(CellState.BLUE);
        else
            setState(CellState.RED);
    }

    public boolean sameAgentState(AgentState.AgentRace race) {
        if ((state == CellState.BLUE && race == AgentState.AgentRace.BLUE) ||
                (state == CellState.RED && race == AgentState.AgentRace.RED))
            return true;
        return false;
    }

}
