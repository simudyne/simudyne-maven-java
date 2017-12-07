package org.example.models.conway;

import providence.simucore.Model;
import providence.simucore.abm.AgentSystem;
import providence.simucore.abm.LongAccumulator;
import providence.simucore.abm.topology.Group;
import providence.simucore.abm.topology.linker.GridConnected;
import providence.simucore.annotations.Variable;

public class GameOfLife implements Model {
    private AgentSystem grid = AgentSystem.create();
    private LongAccumulator bornAccumulator = grid.createLongAccumulator("born");
    private LongAccumulator diedAccumulator = grid.createLongAccumulator("died");

    @Variable
    public long aliveCells() {
        return grid.selectAgents(Cell.class).filter(agent -> agent.alive).count();
    }

    @Variable
    public long born() {
        return bornAccumulator.value();
    }

    @Variable
    public long died() {
        return diedAccumulator.value();
    }

    public void setup() {
        Group cellsGroup = grid.getTopology().generateGroup(100 * 100, init -> {
            boolean alive = init.getPrng().uniform(0.0, 1.0).sample() < 0.25;

            init.spawn(new Cell(alive));
        });

        cellsGroup.connect(new GridConnected().wrapped().mooreConnected());

        grid.setup(new Messages.Start());
    }

    public void calculate() {
        bornAccumulator.reset();
        diedAccumulator.reset();

        grid.calculate(new Messages.Start());
    }
}
