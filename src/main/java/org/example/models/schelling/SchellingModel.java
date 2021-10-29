package org.example.models.schelling;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;

@ModelSettings(macroStep = 50)
public class SchellingModel extends AgentBasedModel<SchellingModel.Globals> {

    public static final class Globals extends GlobalState {
        @Input(name = "Grid Size")
        public int gridSize = 40;

        @Input(name = "Empty Cells Proportion")
        public double emptyCellsPcg = 0.01;

        @Input(name = "Similarity Threshold")
        public double similarityThreshold = 0.4;

        @Input(name = "Data Export Tick")
        public int dataExportTick = 49;

        public GridParameters gridParameters;
    }

    @Override
    public void init() {
        registerAgentTypes(Environment.class, SchellingAgent.class, BlueAgent.class, RedAgent.class);
        registerLinkTypes(Links.SchellingToEnvironmentLink.class, Links.EnvironmentToSchellingLink.class);
        registerMessageTypes(Messages.StateMessage.class, Messages.UnhappyMessage.class);
    }

    @Override
    public void setup() {
        getGlobals().gridParameters = new GridParameters(getGlobals().gridSize, getGlobals().emptyCellsPcg);

        Group<Environment> environmentGroup = generateGroup(Environment.class, 1, Environment::initEnvironment);

        Group<BlueAgent> blueAgentGroup = generateGroup(BlueAgent.class, getGlobals().gridParameters.nbBlue,
                blueAgent -> blueAgent.similarityThreshold = getGlobals().similarityThreshold);
        Group<RedAgent> redAgentGroup = generateGroup(RedAgent.class, getGlobals().gridParameters.nbRed,
                redAgent -> redAgent.similarityThreshold = getGlobals().similarityThreshold);

        environmentGroup.fullyConnected(blueAgentGroup, Links.EnvironmentToSchellingLink.class);
        environmentGroup.fullyConnected(redAgentGroup, Links.EnvironmentToSchellingLink.class);
        blueAgentGroup.fullyConnected(environmentGroup, Links.SchellingToEnvironmentLink.class);
        redAgentGroup.fullyConnected(environmentGroup, Links.SchellingToEnvironmentLink.class);

        super.setup();
    }

    @Override
    public void step() {

        if (getContext().getTick() == 0) {
            run(SchellingAgent.registerToEnvironment(), Environment.receiveRegistrations());
            run(Environment.initPositions());
        }
        run(Environment.writeData());
        run(Environment.updateAgentStates(), SchellingAgent.updateState());
        run(SchellingAgent.step(), Environment.moveAgents());
        if (getContext().getTick() == getGlobals().dataExportTick) {
            run(Environment.exportJSONOutput());
        }
    }
}
