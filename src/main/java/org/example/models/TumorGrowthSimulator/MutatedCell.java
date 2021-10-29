package org.example.models.TumorGrowthSimulator;



import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;

import java.util.Map;
import java.util.Optional;

public class MutatedCell extends Cell {


    private static final double MCProb = 0.4;
    private static final double transitionEnergy = 0.7;
    public long newCellID;


    @Variable
    public double size = 100;

    public static Action<MutatedCell> updateType = Action.create(MutatedCell.class, cell -> {
        if (cell.getPrng().uniform(0, 1).sample() < mutationProbability) {
            mutatedToCancerTransition(cell);
        } else {
            double nbOfCancer = cell.patchStates.values().stream().filter(t -> t == PatchState.CANCER_OCCUPIED).count();
            double nbOccupied = cell.patchStates.values().stream().filter(t -> t != PatchState.UNOCCUPIED).count();
            if (cell.getPrng().uniform(0, 1).sample() < MCProb * (nbOfCancer / nbOccupied)) {
                mutatedToCancerTransition(cell);
            }
        }
    });

    private static void mutatedToCancerTransition(MutatedCell cell) {
        long environment = cell.getLinks(Links.CellToEnvironmentLink.class).get(0).getTo();
        cell.spawn(CancerCell.class, cancer -> {
            cancer.addLink(environment, Links.CellToEnvironmentLink.class);
            cell.newCellID = cancer.getID();
            cancer.patchStates = cell.patchStates;
        });
        cell.send(Messages.UpdatePatchState.class, msg -> {
            msg.patchState = PatchState.CANCER_OCCUPIED;
            msg.cellID = cell.newCellID;
        }).to(environment);
        cell.stop();
    }

    public static Action<MutatedCell> updateCell = Action.create(MutatedCell.class, cell -> {

        if (cell.energyLevel > transitionEnergy) {

            Optional<Long> maybeEmptyPatch = cell.patchStates.entrySet().stream()
                    .filter(a -> a.getValue() == PatchState.UNOCCUPIED)
                    .findAny()
                    .map(Map.Entry::getKey);

            maybeEmptyPatch.ifPresent(emptyId -> {

                double probabilityOfDivision = transitionEnergy * cell.energyLevel;
                double chanceOfDivision = cell.getPrng().uniform(0, 1).sample();

                // Free space, enough energy
                if (probabilityOfDivision > chanceOfDivision) {
                    cell.energyLevel = cell.energyLevel - transitionEnergy;
                    double newCellEnergyLevel = cell.energyLevel / 2;
                    cell.energyLevel = cell.energyLevel / 2;
                    cell.spawn(MutatedCell.class, cancerCell -> {
                        cancerCell.energyLevel = newCellEnergyLevel;
                        cancerCell.addLink(emptyId, Links.CellToEnvironmentLink.class);
                        cell.newCellID = cancerCell.getID();
                    });
                    cell.send(Messages.UpdatePatchState.class, mes -> {
                        mes.cellID = cell.newCellID;
                        mes.patchState = PatchState.MUTATED_OCCUPIED;
                    }).to(emptyId);
                }

                // No free space, enough energy = cell moves to the neighbour
                else {
                    cell.removeLinksTo(cell.getLinks(Links.CellToEnvironmentLink.class).get(0).getTo());
                    cell.addLink(emptyId, Links.CellToEnvironmentLink.class);
                }

            });
        }
    });
}
