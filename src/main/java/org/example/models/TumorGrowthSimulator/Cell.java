package org.example.models.TumorGrowthSimulator;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

import java.util.HashMap;
import java.util.Map;

public abstract class Cell extends Agent<TumorGrowthModel.Globals> {


    public static final double mutationProbability = 0.05;

    public double energyLevel;
    public Map<Long, PatchState> patchStates = new HashMap<>();

    public static Action<Cell> askForNutrients =
            Action.create(Cell.class, cell -> {
                cell.getLinks(Links.CellToEnvironmentLink.class).send(Messages.AskForNutrients.class);
            });

    public static Action<Cell> receiveNutrientsAndPossiblyDie = Action.create(Cell.class, cell -> {
        cell.getMessagesOfType(Messages.NutrientReply.class).forEach(mes -> {
            if (mes.getBody() == 0 && cell.getPrng().uniform(0, 1).sample() < (1 - cell.energyLevel)) {
                cell.getLinks(Links.CellToEnvironmentLink.class).send(Messages.PatchVacant.class);
                cell.stop();
            } else {
                cell.energyLevel = cell.energyLevel + Math.min(mes.getBody(), 0.02);
            }
        });
    });


    public static Action<Cell> updateNeighbourKnowledge = Action.create(Cell.class, cell -> {
        cell.patchStates = cell.getMessagesOfType(Messages.NeighborhoodStates.class).get(0).patchStateMap;
    });


}
