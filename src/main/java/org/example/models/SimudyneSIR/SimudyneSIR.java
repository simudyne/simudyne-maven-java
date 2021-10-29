package org.example.models.SimudyneSIR;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;

public class SimudyneSIR extends AgentBasedModel<SimudyneSIR.Globals> {

    @Input(name = "Number of SIR Agents")
    public int numAgents = 100;

    public static final class Globals extends GlobalState {
        @Input
        public double infectionProbability = 0.01;

        @Input
        public int initialOutbreak = 1;

        @Input
        public int infectionDuration = 1;
    }

    @Override
    public void init() {

        // note: don't actually need to save these if they're not persistent
        createLongAccumulator("Susceptible");
        createLongAccumulator("Infected");
        createLongAccumulator("Recovered");

        registerAgentTypes(SirAgent.class);
        registerLinkTypes(Links.SirLink.class);
    }

    @Override
    public void setup() {
        // generate group
//        Group<SirAgent> sirAgentGroup = generateGroup(SirAgent.class, getGlobals().numAgents);
        Group<SirAgent> sirAgentGroup = generateGroup(SirAgent.class, numAgents, a -> {
            if (a.getID() < getGlobals().initialOutbreak)
            {
                // expose
                a.status = SirAgent.Status.INFECTED;
            }
        });
        sirAgentGroup.fullyConnected(sirAgentGroup, Links.SirLink.class);



        super.setup();
    }

    @Override
    public void step() {
        super.step();

        run(SirAgent.step, SirAgent.exposed);

    }
}
