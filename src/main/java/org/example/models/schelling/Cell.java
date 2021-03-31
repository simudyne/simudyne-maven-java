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
        setState(race == AgentState.AgentRace.BLUE ? CellState.BLUE : CellState.RED);
        occupied = true;
    }

    public void vacate() {
        setState(CellState.EMPTY);
        occupied = false;
    }

    public boolean sameAgentState(AgentState.AgentRace race) {
        return (state == CellState.BLUE && race == AgentState.AgentRace.BLUE) ||
                (state == CellState.RED && race == AgentState.AgentRace.RED);
    }

}
