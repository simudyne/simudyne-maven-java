package org.example.models.TumorGrowthSimulator;




import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;

import java.util.Map;
import java.util.Optional;

public class CancerCell extends Cell {

    private static final double CMProb = 0.8;
    private static final double divisionEnergy = 0.6;
    public long newCellID;


    @Variable
    public double size = 100;

    public static Action<CancerCell> updateType = Action.create(CancerCell.class, cell -> {

            double nbNormalAndMutated = cell.patchStates.values().stream()
                    .filter(t -> t == PatchState.MUTATED_OCCUPIED || t == PatchState.NORMAL_OCCUPIED)
                    .count();

            double nbOccupied = cell.patchStates.values().stream()
                    .filter(t -> t != PatchState.UNOCCUPIED)
                    .count();

            if (cell.getPrng().uniform(0, 1).sample() <  CMProb * (nbNormalAndMutated/nbOccupied)) {
                cancerToMutatedTransition(cell);
            }
    });




    private static void cancerToMutatedTransition(CancerCell cell) {
        long environment = cell.getLinks(Links.CellToEnvironmentLink.class).get(0).getTo();
        cell.spawn(MutatedCell.class, mutated -> {
            mutated.addLink(environment, Links.CellToEnvironmentLink.class);
            mutated.patchStates = cell.patchStates;
            cell.newCellID = mutated.getID();
        });
        cell.send(Messages.UpdatePatchState.class, msg ->{
            msg.patchState = PatchState.MUTATED_OCCUPIED;
            msg.cellID = cell.newCellID;
        }).to(environment);
        cell.stop();
    }

    public static Action<CancerCell> updateCell = Action.create(CancerCell.class, cell -> {
                if (cell.energyLevel > divisionEnergy ) {

                    Optional<Long> maybeEmptyPatch = cell.patchStates.entrySet().stream()
                            .filter(a -> a.getValue() == PatchState.UNOCCUPIED)
                            .findAny()
                            .map(Map.Entry::getKey);

                    maybeEmptyPatch.ifPresent(emptyId -> {

                        double probabilityOfDivision =  divisionEnergy * cell.energyLevel;
                        double chanceOfDivision = cell.getPrng().uniform(0, 1).sample();

                        // Free space, enough energy
                        if (probabilityOfDivision > chanceOfDivision) {
                            cell.energyLevel = cell.energyLevel - divisionEnergy;
                            double newCellEnergyLevel = cell.energyLevel / 2;
                            cell.energyLevel = cell.energyLevel / 2;
                            cell.spawn(CancerCell.class, cancerCell -> {
                                cancerCell.energyLevel = newCellEnergyLevel;
                                cancerCell.addLink(emptyId, Links.CellToEnvironmentLink.class);
                                cell.newCellID = cancerCell.getID();
                            });
                            cell.send(Messages.UpdatePatchState.class, mes -> {
                                mes.cellID = cell.newCellID;
                                mes.patchState = PatchState.CANCER_OCCUPIED;
                            }).to(emptyId);
                        }

                        // No free space, enough energy = cell moves to the neighbour
                        else {
                            cell.removeLinksTo(cell.getLinks(Links.CellToEnvironmentLink.class).get(0).getTo());
                            cell.addLink(emptyId ,Links.CellToEnvironmentLink.class);
                        }

                    });
                }
            });
}
