package org.example.models.schelling;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

public abstract class SchellingAgent extends Agent<SchellingModel.Globals> {
    public SchellingAgent(AgentState.AgentRace race) {
        this.race = race;
    }

    public final AgentState.AgentRace race;

    public double similarityThreshold;

    public AgentState state;

    public static Action<SchellingAgent> step() {
        return Action.create(SchellingAgent.class, schellingAgent -> {
            schellingAgent.state.changeSatisfaction(schellingAgent.state.similarityMetric < schellingAgent.similarityThreshold
                    ? AgentState.Satisfaction.UNHAPPY : AgentState.Satisfaction.HAPPY);

            if (schellingAgent.state.satisfaction == AgentState.Satisfaction.UNHAPPY) {
                schellingAgent.getLinks(Links.SchellingToEnvironmentLink.class)
                        .send(Messages.UnhappyMessage.class);
            }
        });
    }

    public static Action<SchellingAgent> registerToEnvironment() {
        return Action.create(SchellingAgent.class, schellingAgent -> {
            schellingAgent.state = new AgentState(schellingAgent.race);
            AgentState state = new AgentState(schellingAgent.race);
            schellingAgent.getLinks(Links.SchellingToEnvironmentLink.class).send(Messages.StateMessage.class,
                    (msg, link) -> msg.state = state);
        });
    }

    public static Action<SchellingAgent> updateState() {
        return Action.create(SchellingAgent.class, schellingAgent ->
                schellingAgent.state.update(schellingAgent.getMessageOfType(Messages.StateMessage.class).state));
    }
}
