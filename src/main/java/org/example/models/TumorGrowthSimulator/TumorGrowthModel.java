package org.example.models.TumorGrowthSimulator;


import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.Split;
import simudyne.core.annotations.Input;
import simudyne.core.graph.LongAccumulator;

public class TumorGrowthModel extends AgentBasedModel<TumorGrowthModel.Globals> {

    public static final class Globals extends GlobalState {
        public int ticks = 0;

        @Input
        public int nbEnvironmentPatches = 2500;

        @Input
        public double diffusionWeight = 0.08;



        private double initialRatioOfNCellsToPatches = 0.08;
        private double initialRatioOfBloodPatchesToPatches = 0.015;

    }

    LongAccumulator nbDeadNormalCells = createLongAccumulator("nbDeadNormalCells");
    LongAccumulator nbDeadMutatedCells = createLongAccumulator("nbDeadMutatedCells");
    LongAccumulator nbDeadCancerCells = createLongAccumulator("nbDeadCancerCells");

    @Override
    public void init() {

        registerAgentTypes(NormalCell.class, MutatedCell.class, CancerCell.class, EnvironmentPatches.class, BloodVessel.class);
        registerLinkTypes(Links.CellToCellLink.class, Links.CellToEnvironmentLink.class, Links.EnvironmentToEnvironmentLink.class);
    }

    @Override
    public void setup() {

        Group<EnvironmentPatches> environmentPatchesGroup = generateGroup(EnvironmentPatches.class, getGlobals().nbEnvironmentPatches, environmentPatches -> {
            environmentPatches.initialRatioOfNCellsToPatches = getGlobals().initialRatioOfNCellsToPatches;
            environmentPatches.isBloodPatch = environmentPatches.getPrng().uniform(0,1).sample() < getGlobals().initialRatioOfBloodPatchesToPatches;
            environmentPatches.nutrientLevel = environmentPatches.getPrng().uniform(0,1).sample();
            environmentPatches.maxNutrientLevel = environmentPatches.nutrientLevel;
        });


       environmentPatchesGroup.gridConnected(Links.EnvironmentToEnvironmentLink.class);

        super.setup();
    }

    @Override
    public void step() {
        super.step();
        getGlobals().ticks++;



        // spawn initial normal cells
        if (getGlobals().ticks == 1) {
            run(EnvironmentPatches.spawnCell);
        }

        else {

            /* *******************
             * Cell update phase *
             ****************** */

            // gets the nutrients from associated patch
            run(Cell.askForNutrients, EnvironmentPatches.sendNutrients, Cell.receiveNutrientsAndPossiblyDie, EnvironmentPatches.acknowledgeNewVacancy);

            run(EnvironmentPatches.sendState, EnvironmentPatches.updateNeighborStates);

            run(EnvironmentPatches.sendNeighborStates,  Cell.updateNeighbourKnowledge);

            run( Split.create(
                    CancerCell.updateCell,
                    NormalCell.updateCell,
                    MutatedCell.updateCell),
                    EnvironmentPatches.acknowledgeSpawnedCell);

            /* ********************
             * Patch update phase *
             ****************** */
            run(BloodVessel.sendBloodVesselReplenish, EnvironmentPatches.receiveBloodVesselReplenish);

            run(EnvironmentPatches.sendDiffusionReplenish, EnvironmentPatches.receiveDiffusionReplenish);


            /* ********************
             * Type update phase *
             ****************** */
            run(EnvironmentPatches.sendState, EnvironmentPatches.updateNeighborStates);

            run(EnvironmentPatches.sendNeighborStates, Cell.updateNeighbourKnowledge);
            run(Split.create(
                    NormalCell.updateType,
                    MutatedCell.updateType,
                    CancerCell.updateType),
                    EnvironmentPatches.acknowledgeSpawnedCell
            );
        }
    }
}
