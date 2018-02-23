package org.example.models.conway;

import simudyne.core.Model;
import simudyne.core.abm.Action;
import simudyne.core.abm.AgentSystem;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Sequence;
import simudyne.core.abm.topology.Group;
import simudyne.core.annotations.*;
import simudyne.core.graph.BlankLink;
import simudyne.core.graph.LongAccumulator;

@ModelSettings(macroStep = 25)
public class GameOfLife implements Model {
  public static class Globals extends GlobalState {
    @Constant public float initiallyAlive = 0.25f;
  }

  @Custom public AgentSystem<Globals> grid = AgentSystem.create(new Globals());

  private LongAccumulator bornAccumulator = grid.createLongAccumulator("born");
  private LongAccumulator diedAccumulator = grid.createLongAccumulator("died");

  @Variable
  public long aliveCells() {
    return grid.select(Cell.class).filter(agent -> agent.alive).count();
  }

  @Constant
  public int gridSize = 20;

  public void setup() {
    Group<Cell> cellsGroup =
        grid.generateGroup(
            Cell.class,
            gridSize * gridSize,
            cell -> {
              cell.alive =
                  cell.getPrng().uniform(0.0, 1.0).sample() < cell.getGlobals().initiallyAlive;
            });

    cellsGroup.gridConnected(BlankLink.class).wrapped();

    grid.setup();
  }

  public void step() {
    bornAccumulator.reset();
    diedAccumulator.reset();

    Sequence.create(
            Action.create(Cell.class, Cell::onStart),
            Action.create(Cell.class, Cell::onNeighbourMessages))
        .run(grid);
  }
}
