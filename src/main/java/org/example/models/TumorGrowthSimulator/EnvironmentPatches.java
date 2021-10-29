package org.example.models.TumorGrowthSimulator;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentPatches extends Agent<TumorGrowthModel.Globals> {

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


    /*
    This action is to be called once at the start of the simulation. Patches have some finite probability of
    spawning a cell dependant on the desired initial ratios. All cells are initially spawned as 'Normal'/
     */
    public static Action<EnvironmentPatches> spawnCell =
            Action.create(EnvironmentPatches.class,
                    envPatch -> {
                        if (envPatch.isBloodPatch) {
                            envPatch.spawn(BloodVessel.class, bloodVessel -> {
                                bloodVessel.initialRatioOfNCellsToPatches = envPatch.initialRatioOfNCellsToPatches;
                                bloodVessel.isBloodPatch = true;
                                bloodVessel.nutrientLevel = envPatch.nutrientLevel;
                                bloodVessel.maxNutrientLevel = envPatch.maxNutrientLevel;
                                bloodVessel.patchState = envPatch.patchState;
                                envPatch.getLinks(Links.EnvironmentToEnvironmentLink.class).forEach(environmentToEnvironmentLink -> {
                                    bloodVessel.addLink(environmentToEnvironmentLink.getTo(), Links.EnvironmentToEnvironmentLink.class);
                                });
                                //bloodVessel.addLink(envPatch.cellID, Links.CellToEnvironmentLink.class);
                            });
                            envPatch.stop();
                        }
                        //boolean shouldSpawn = envPatch.getPrng().uniform(0,1).sample() < envPatch.initialRatioOfNCellsToPatches;
                        boolean shouldSpawn = envPatch.getPrng().uniform(0, 1).sample() < 0.5;
                        if (shouldSpawn) {
                            envPatch.spawn(NormalCell.class, normalCell -> {
                                normalCell.energyLevel = normalCell.getPrng().uniform(0, 1).sample();
                                normalCell.addLink(envPatch.getID(), Links.CellToEnvironmentLink.class);
                                envPatch.cellID = normalCell.getID();
                            });
                            envPatch.patchState = PatchState.NORMAL_OCCUPIED;
                        }
                    });

    /*
    Receives requests from cells to find out how much nutrients are available and sends the value back.
    The state of the patch is then updated to the maximum of the delta or zero. This reflects the fact
    that cells only consume what is available to them.
     */
    public static Action<EnvironmentPatches> sendNutrients =
            Action.create(EnvironmentPatches.class, envPatch -> {
                envPatch.getMessagesOfType(Messages.AskForNutrients.class).forEach(mes -> {
                    envPatch.send(Messages.NutrientReply.class, envPatch.nutrientLevel).to(mes.getSender());
                    // TODO hard coded at the moment, improve if needed.
                    envPatch.nutrientLevel = Math.max(envPatch.nutrientLevel - unitOfNutrientConsumption, 0);
                });
            });

    /*
    Message sends a special message to 8 nearest neighbours if the patch is a blood vessel this acts to
    replenish neighbours.
     */


    /*
    If the environment is near a blood vessel and thus receives a BloodVesselReplenish message then replenish the
    nutrient levels to their maximum amount.
     */
    public static Action<EnvironmentPatches> receiveBloodVesselReplenish = Action.create(EnvironmentPatches.class, envPatch -> {
        if (envPatch.getMessagesOfType(Messages.BloodVesselReplenish.class).size() > 0) {
            envPatch.nutrientLevel = envPatch.maxNutrientLevel;
        }
    });


    // TODO look at this not 100% sure if you need to reduce the amount
    public static Action<EnvironmentPatches> sendDiffusionReplenish = Action.create(EnvironmentPatches.class, envPatch -> {
        double totalNutrientsToSend = diffusionWeight * envPatch.nutrientLevel;
        double nutrientToSend = totalNutrientsToSend / 8;
        envPatch.getLinks(Links.EnvironmentToEnvironmentLink.class).send(Messages.DiffusionReplenish.class, nutrientToSend);
        envPatch.nutrientLevel -= totalNutrientsToSend;
    });

    public static Action<EnvironmentPatches> receiveDiffusionReplenish = Action.create(EnvironmentPatches.class, envPatch -> {
        envPatch.getMessagesOfType(Messages.DiffusionReplenish.class).forEach(mes -> {
            envPatch.nutrientLevel += mes.getBody();
        });
    });


    public static Action<EnvironmentPatches> sendState =
            Action.create(EnvironmentPatches.class,
                    envPatch -> {
                        envPatch.getLinks(Links.EnvironmentToEnvironmentLink.class).send(Messages.StateMessage.class, (msg, link) -> {
                            msg.patchState = envPatch.patchState;
                            msg.patchID = envPatch.getID();
                        });
                    });


    private Map<Long, PatchState> patchStates = new HashMap<>();


    public static Action<EnvironmentPatches> updateNeighborStates =
            Action.create(EnvironmentPatches.class,
                    envPatch -> {
                        envPatch.patchStates.clear();
                        envPatch.getMessagesOfType(Messages.StateMessage.class).forEach(msg -> {
                            long patchID= msg.patchID;
                            PatchState patchState1 = msg.patchState;
                            envPatch.patchStates.put(patchID, patchState1);
                        });
                    });

    public static Action<EnvironmentPatches> sendNeighborStates =
            Action.create(EnvironmentPatches.class,
                    envPatch -> {
                        if (envPatch.patchState != PatchState.UNOCCUPIED) {
                            envPatch.send(Messages.NeighborhoodStates.class, msg -> {
                                msg.patchStateMap = envPatch.patchStates;
                            }).to(envPatch.cellID);
                        }
                    });

    public static Action<EnvironmentPatches> acknowledgeNewVacancy =
            Action.create(EnvironmentPatches.class,
                    envPatch -> {
                        if (envPatch.hasMessageOfType(Messages.PatchVacant.class)) {
                            envPatch.patchState = PatchState.UNOCCUPIED;
                            envPatch.cellID = -1;
                        }
                    });

    public static Action<EnvironmentPatches> acknowledgeSpawnedCell =
            Action.create(EnvironmentPatches.class,
                    envPatch -> {
                        if (envPatch.hasMessageOfType(Messages.UpdatePatchState.class)) {
                            // TODO Need to check that another cell has not already spawned in the same phase
                            envPatch.patchState = envPatch.getMessagesOfType(Messages.UpdatePatchState.class).get(0).patchState;
                            envPatch.cellID = envPatch.getMessagesOfType(Messages.UpdatePatchState.class).get(0).cellID;
                        }
                    });


}





