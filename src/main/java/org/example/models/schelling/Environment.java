package org.example.models.schelling;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.HashMap;
import java.util.Optional;

public class Environment extends Agent<SchellingModel.Globals> {
    public HashMap<Long, AgentState> agentMap;

    public Grid grid;

    @Variable
    public double averageSimilarity;

    @Variable
    public int unhappyAgentCount;

    public void initEnvironment() {
        grid = new Grid(getGlobals().gridSize);
        agentMap = new HashMap<>();

        grid.init(getGlobals().gridParameters.nbBlue, getGlobals().gridParameters.nbRed);
    }

    public static Action<Environment> receiveRegistrations() {
        return Action.create(Environment.class, environment ->
                environment.getMessagesOfType(Messages.StateMessage.class).forEach(stateMessage ->
                        environment.agentMap.put(stateMessage.getSender(), stateMessage.state)));
    }

    public static Action<Environment> initPositions() {
        return Action.create(Environment.class, environment -> environment.agentMap.forEach((agentID, agentState) -> {
            Optional<Cell> optionalCell = environment.grid.cellList.stream()
                    .filter(x -> x.sameAgentState(agentState.race) && !x.occupied).findFirst();

            if (!optionalCell.isPresent())
                throw new IllegalArgumentException("No position found.");

            Cell cell = optionalCell.get();
            cell.switchState(agentState.race);
            agentState.position = cell;
        }));
    }

    public double calculateSimilarityMetric(Coordinates coordinates, AgentState.AgentRace agentRace) {
        int neighbours = 0;
        int similarNeighbours = 0;
        double similarityMetric = 0.0;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                Coordinates newCoordinates = new Coordinates(coordinates.x + i, coordinates.y + j);
                if (newCoordinates.x >= 0 && newCoordinates.y < grid.gridSize &&
                        newCoordinates.y >= 0 && newCoordinates.x < grid.gridSize && !(i == 0 && j == 0)) {
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

    public static Action<Environment> moveAgents() {
        return Action.create(Environment.class, environment -> {
            environment.unhappyAgentCount = 0;
            environment.getMessagesOfType(Messages.UnhappyMessage.class).stream().unordered().forEach(msg -> {
                        environment.unhappyAgentCount += 1;
                        AgentState.AgentRace race = environment.agentMap.get(msg.getSender()).race;
                        Optional<Cell> optionalCell = environment.grid.cellList.stream().filter(x ->
                                !x.occupied && environment.calculateSimilarityMetric(x.coordinates, race) >=
                                        environment.getGlobals().similarityThreshold).findAny();

                        if (!optionalCell.isPresent())
                            return;

                        Cell oldCell = environment.agentMap.get(msg.getSender()).position;
                        oldCell.vacate();

                        Cell newCell = optionalCell.get();
                        newCell.switchState(race);

                        environment.agentMap.get(msg.getSender()).changePosition(newCell);
                    }
            );
        });
    }


    public static Action<Environment> updateAgentStates() {
        return Action.create(Environment.class, environment -> {
            environment.averageSimilarity = 0;
            environment.agentMap.forEach((agentID, agentState) -> {
                double similarityMetric = environment.calculateSimilarityMetric(agentState.position.coordinates, agentState.race);
                agentState.changeSimilarityMetric(similarityMetric);
                environment.averageSimilarity += similarityMetric;

                environment.getLinksTo(agentID).send(Messages.StateMessage.class, (msg, link) ->
                        msg.state = agentState);
            });
            environment.averageSimilarity = environment.averageSimilarity / environment.agentMap.size();
        });
    }

}