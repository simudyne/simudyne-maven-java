package org.example.models.schelling;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

public abstract class SchellingAgent extends Agent<Environment> {
    public SchellingAgent(AgentState.AgentRace race) {
        this.race = race;
    }

    public final AgentState.AgentRace race;

    public double similarityThreshold;

    public AgentState state;

    public static Action<SchellingAgent> checkSatisfaction() {
        return Action.create(SchellingAgent.class, schellingAgent -> {
            schellingAgent.state.changeSatisfaction(schellingAgent.state.similarityMetric < schellingAgent.similarityThreshold
                    ? AgentState.Satisfaction.UNHAPPY : AgentState.Satisfaction.HAPPY);
        });
    }

    public static Action<SchellingAgent> move() {
        return Action.create(SchellingAgent.class, schellingAgent -> {
            if (schellingAgent.state.satisfaction == AgentState.Satisfaction.UNHAPPY){
                schellingAgent.getGlobals().moveAgent(schellingAgent.state);
            }
        });
    }


    public static Action<SchellingAgent> register() {
        return Action.create(SchellingAgent.class, schellingAgent -> {
            schellingAgent.state = new AgentState(schellingAgent.race);
            schellingAgent.getGlobals().register(schellingAgent.getID(), schellingAgent.state);
        });
    }

    public static Action<SchellingAgent> updateState() {
        return Action.create(SchellingAgent.class, schellingAgent -> {
            schellingAgent.state.update(schellingAgent.getGlobals().getAgentState(schellingAgent.getID()));

            schellingAgent.getLongAccumulator("numUnhappy").add(schellingAgent.state.satisfaction == AgentState.Satisfaction.UNHAPPY ? 1 : 0);
            schellingAgent.getDoubleAccumulator("avSimilarity").add(schellingAgent.state.similarityMetric);
        });
    }
}
