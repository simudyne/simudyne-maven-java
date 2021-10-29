package org.example.models.forestFire;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;


@ModelSettings(macroStep = 30)
public class ForestFireModel extends AgentBasedModel<ForestFireModel.Globals> {

    @Input(name = "Number of Agents")
    public int numAgents = 100;

    public static final class Globals extends GlobalState {
        public int ticks = 0;

        @Input(name = "gridSize")
        public int gridSize = 20;

        @Input(name = "Empty Cells Proportion")
        public double emptyCellsPcg = 0.4;

        @Input(name = "Probability of spontaneous ignition")
        public double probabilityIgnite = 0.01;

        @Input(name = "Ignite side located (1=yes)")
        public int locatedIgnite = 1;

        @Input(name = "Neighborhood type: 1 Moore; 2= Von Neumann; 3=customized")
        public int neighborhood = 2;

        @Input(name = "Neighbors distance")
        public int similarityThreshold = 2;

        public GridParameters gridParameters;
    }

    @Override
    public void init() {
        registerAgentTypes(ContagionAgent.class);
        registerLinkTypes(Links.AgentLink.class);
        registerMessageTypes(Messages.AttributeMessage.class, Messages.StateMessage.class, Messages.SendContagion.class);
//
//        getContext()
//                .getChannels()
//                .createOutputChannel()
//                .setId("contagion_output")
//                .setSchema(Monitor.getMonitorSchema())
//                .addLabel("simudyne:parquet")
//                .build();
    }


    @Override
    public void setup() {
        getGlobals().gridSize = (int) Math.sqrt(numAgents);

        getGlobals().gridParameters = new GridParameters(getGlobals().gridSize, getGlobals().emptyCellsPcg);

        Group<ContagionAgent> agentGroup = generateGroup(ContagionAgent.class, getGlobals().gridParameters.nbTotal, agents -> {
            Cell newCell = getGlobals().gridParameters.assignCell();
            if(getGlobals().neighborhood != 1 && getGlobals().neighborhood != 2) {
                agents.attribute1 = newCell.getXCoordinate();
                agents.attribute2 = newCell.getYCoordinate();
            }
            agents.initializeState(newCell.isEmpty());
            agents.frequencyIgnition = getGlobals().probabilityIgnite;
        });

        if(getGlobals().neighborhood == 1) {
            agentGroup.gridConnected(Links.AgentLink.class).mooreConnected();
        } else if(getGlobals().neighborhood == 2){
            agentGroup.gridConnected(Links.AgentLink.class).vonNeumannConnected();
        } else {
            agentGroup.fullyConnected(agentGroup, Links.AgentLink.class);
        }

        super.setup();
    }

    @Override
    public void step() {
        super.step();
        getGlobals().ticks++;
        if (getContext().getTick() == 0) {
            if(getGlobals().neighborhood !=1 && getGlobals().neighborhood != 2){
                run(ContagionAgent.informNeighborAttribute(), ContagionAgent.pruneConnection());
            }

            if(getGlobals().locatedIgnite==1) {
                run(ContagionAgent.locatedIgnite());
            } else {
                run(ContagionAgent.spontaneousIgnite());
            }

            return;
        }

        run(ContagionAgent.sendContagion(), ContagionAgent.receiveContagion());
//        run(ContagionAgent.writeData());

    }

}


