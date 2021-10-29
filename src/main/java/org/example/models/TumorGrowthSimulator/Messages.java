package org.example.models.TumorGrowthSimulator;


import simudyne.core.graph.Message;

import java.util.Map;

public class Messages {

    public static class SpawnNormal extends Message {
    }

    public static class AskForNutrients extends Message {
    }

    public static class NutrientReply extends Message.Double {
    }


    public static class BloodVesselReplenish extends Message {

    }

    public static class DiffusionReplenish extends Message.Double {

    }

    public static class NeighborhoodStates extends Message {
        public Map< java.lang.Long, PatchState> patchStateMap;
    }

    public static class StateMessage extends Message {
        public PatchState patchState;
        public long patchID;
    }

    public static class PatchVacant extends Message {
        public long vacantPatchID;
    }

    public static class FreePatchAvailable extends Message {
        public long patchID;
    }

    public static class UpdatePatchState extends Message {
        public PatchState patchState;
        public long cellID;
    }
}
