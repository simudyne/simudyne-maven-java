package org.example.models.TumorGrowthSimulator;

import simudyne.core.abm.Action;
import simudyne.core.annotations.Variable;

import java.util.Map;
import java.util.Optional;

public class NormalCell extends Cell {

    private static final double transitionEnergy = 0.7;
    public long newCellID;


    @Variable
    public double size = 100;

    public static Action<NormalCell> updateType = Action.create(NormalCell.class, cell -> {
        if (cell.getPrng().uniform(0, 1).sample() < mutationProbability) {
            long environment = cell.getLinks(Links.CellToEnvironmentLink.class).get(0).getTo();

            cell.spawn(MutatedCell.class, mutated -> {
                mutated.addLink(environment, Links.CellToEnvironmentLink.class);
                cell.newCellID = mutated.getID();
                mutated.patchStates = cell.patchStates;
            });
            cell.send(Messages.UpdatePatchState.class, msg -> {
                msg.patchState = PatchState.MUTATED_OCCUPIED;
                msg.cellID = cell.newCellID;
            }).to(environment);
            cell.stop();
        }
    });


    public static Action<NormalCell> updateCell = Action.create(NormalCell.class, cell -> {


        Optional<Long> maybeEmptyPatch = cell.patchStates.entrySet().stream()
                .filter(a -> a.getValue() == PatchState.UNOCCUPIED)
                .findAny()
                .map(Map.Entry::getKey);

        maybeEmptyPatch.ifPresent(v -> {
            if (cell.energyLevel > transitionEnergy) {
                double probabilityOfDivision = transitionEnergy * cell.energyLevel;
                double chanceOfDivision = cell.getPrng().uniform(0, 1).sample();

                if (probabilityOfDivision > chanceOfDivision) {
                    cell.energyLevel = cell.energyLevel - transitionEnergy;
                    double newCellEnergyLevel = cell.energyLevel / 2;
                    cell.energyLevel = cell.energyLevel / 2;
                    cell.spawn(NormalCell.class, normalCell -> {
                        normalCell.energyLevel = newCellEnergyLevel;
                        normalCell.addLink(v, Links.CellToEnvironmentLink.class);
                        cell.newCellID = normalCell.getID();
                    });
                    cell.send(Messages.UpdatePatchState.class, mes -> {
                        mes.cellID = cell.newCellID;
                        mes.patchState = PatchState.NORMAL_OCCUPIED;
                    }).to(v);
                }
            }
        });
    });
}