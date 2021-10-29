package org.example.models.forestFire;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.values.ValueRecord;

public class ContagionAgent extends Agent<ForestFireModel.Globals> {
    public int attribute1;
    public int attribute2;
    public double frequencyIgnition;
    public State state;

    @Variable(name="Burning")
    public int burning =0;

    @Variable(name="Burned")
    public int burned =0;

    @Variable
    public String _color() {
        switch (state) {
            case EMPTY:
                return "white";
            case HEALTHY:
                return "green";
            case INFECTED:
                return "red";
            case RESISTANT:
                return "black";
        }

        return "white";
    }


    public void initializeState(boolean isEmpty) {
        state = isEmpty ? State.EMPTY : State.HEALTHY;
    }


    public static Action<ContagionAgent> informNeighborAttribute() {
        return Action.create(ContagionAgent.class, contagionAgent -> contagionAgent.sendAttribute(
                contagionAgent.attribute1, contagionAgent.attribute2));
    }


    public static Action<ContagionAgent> pruneConnection() {
        return Action.create(
                ContagionAgent.class, contagionAgent ->
                        contagionAgent.getMessagesOfType(Messages.AttributeMessage.class).forEach(msg -> {
                            double dissimilarity = Math.pow((msg.attribute1 - contagionAgent.attribute1), 2) + Math.pow((msg.attribute2 - contagionAgent.attribute2), 2);
                            if (dissimilarity > contagionAgent.getGlobals().similarityThreshold) {
                                contagionAgent.removeLinksTo(msg.getSender());
                            }
                        }));
    }


    public static Action<ContagionAgent> spontaneousIgnite() {
        return Action.create(
                ContagionAgent.class, contagionAgent -> {
                    double igniteSpontaneous = Math.random();
                    if (igniteSpontaneous < contagionAgent.frequencyIgnition) {
                        contagionAgent.switchToInfected();
                    }

                });
    }


    public static Action<ContagionAgent> locatedIgnite() {
        return Action.create(
                ContagionAgent.class, contagionAgent -> {
                    if (contagionAgent.getID()<contagionAgent.getGlobals().gridSize) {
                        contagionAgent.switchToInfected();
                    }
                });
    }

    public static Action<ContagionAgent> sendContagion() {
        return Action.create(
                ContagionAgent.class, contagionAgent -> {
                    if(contagionAgent.getContagionState() == State.INFECTED) {
                        contagionAgent.getLinks(Links.AgentLink.class).send(Messages.SendContagion.class);
                        contagionAgent.switchToRecovered();
                    }
                });
    }


    public static Action<ContagionAgent> receiveContagion() {
        return Action.create(
                ContagionAgent.class, contagionAgent ->
                    contagionAgent.getMessagesOfType(Messages.SendContagion.class).forEach(msg ->
                        contagionAgent.switchToInfected()));
    }

    public static Action<ContagionAgent> writeData() {
        return Action.create(
                ContagionAgent.class, contagionAgent -> {
                    Monitor agentMonitor = new Monitor(contagionAgent.getID(), contagionAgent.state);
                    ValueRecord agentOutput = agentMonitor.getMonitorValue();
                    contagionAgent.getContext().getChannels().getOutputChannelWriterById("contagion_output").write(agentOutput);
                });
    }


    public static Action<ContagionAgent> updateState() {
        return Action.create(ContagionAgent.class, contagionAgent -> {
            System.out.println("Sending state message");
            contagionAgent.getLinks(Links.MonitorLink.class).send(Messages.StateMessage.class, (msg, link) -> msg.state = contagionAgent.getContagionState());
        });
    }

    private void switchToRecovered(){
             if (state == State.INFECTED){
                 state = State.RESISTANT;
                 burning = 0;
        }
    }

    private void switchToInfected(){
        if (state == State.HEALTHY){
            state = State.INFECTED;
            burning = 1;
            burned = 1;
        }
    }


    private void sendAttribute(int attribute1, int attribute2){
        getLinks(Links.AgentLink.class).send(Messages.AttributeMessage.class, (msg, link) -> {
            msg.attribute1=attribute1;
            msg.attribute2=attribute2;
        });
    }


    public State getContagionState() {
        return state;
    }
}
