package org.example.models.conway;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;
import simudyne.core.annotations.Variable;

@ModelSettings(macroStep = 25)
public class GameOfLife extends AgentBasedModel<GameOfLife.Globals> {
  public static final class Globals extends GlobalState {
    @Constant float fillFactor = 0.25f;
  }

  @Input boolean distributed = false;
  @Input int gridSize = 20;

  @Variable
  long aliveCells() {
    return select(Cell.class).filter(agent -> agent.alive).count();
  }

  {
    registerAgentType(Cell.class);
    registerMessageTypes(Messages.Start.class, Messages.Alive.class);

    createLongAccumulator("born");
    createLongAccumulator("died");
  }

  public void setup() {
    if (distributed) {
      getConfig()
          .setString(
              "core-abm.backend-implementation", "simudyne.core.graph.spark.SparkGraphBackend");
    }

    Group<Cell> cellsGroup =
        generateGroup(
            Cell.class,
            gridSize * gridSize,
            cell -> {
              cell.alive = cell.getPrng().uniform(0.0, 1.0).sample() < cell.getGlobals().fillFactor;
            });

    cellsGroup.gridConnected(Links.Neighbour.class).wrapped().mooreConnected();

    super.setup();
  }

  public void step() {
    super.step();

    run(Cell.action(Cell::onStart), Cell.action(Cell::onNeighbourMessages));
  }
}
