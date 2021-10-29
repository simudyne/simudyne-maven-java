package org.example.models.TumorGrowthSimulator;


import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

public class BloodVessel extends Agent<TumorGrowthModel.Globals> {

    @Variable
    public double nutrientLevel;
    // TODO improve this we want it to be final/immutable
    public double maxNutrientLevel;
    public double initialRatioOfNCellsToPatches;
    public boolean isBloodPatch;
    public long cellID;

    public int linkCount;

    private static final double diffusionWeight = 0.8;
    private static final double unitOfNutrientConsumption = 0.02;

    public PatchState patchState = PatchState.UNOCCUPIED;


    public static Action<BloodVessel> sendBloodVesselReplenish =
            Action.create(BloodVessel.class, bloodVessel -> {
                    bloodVessel.getLinks(Links.EnvironmentToEnvironmentLink.class).send(Messages.BloodVesselReplenish.class);

            });
}
