package org.example.models.GaiKapadia;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.values.ValueRecord;

import java.util.concurrent.atomic.AtomicInteger;

public class Bank extends Agent<GaiKapadiaModel.Globals> {
    public double cashBuffer;

    public State state = State.SOLVENT;

    public double numberExposedBanks;

    public double percentInterBankAssets;

    @Variable
    public String _color() {
        switch (state) {
            case SOLVENT:
                return "white";
            case DEFAULT:
                return "red";
        }
        return "white";
}


    public static Action<Bank> informNeighbors() {
        return Action.create(Bank.class, bank -> bank.getLinks(Links.AgentLink.class).send(Messages.SendConnection.class));
    }


    public static Action<Bank> pruneConnection(double nodeDegree) {
        return Action.create(
                Bank.class, bank ->
                        bank.getMessagesOfType(Messages.SendConnection.class).forEach(msg -> {
                            if (bank.getPrng().uniform(0, 1).sample() > (nodeDegree/bank.getGlobals().numberBanks)) {
                                bank.removeLinksTo(msg.getSender());
                            }
                        }));
    }


    public static Action<Bank> receiveNeighborInformation() {
        return Action.create(
                Bank.class, bank ->{
                    AtomicInteger dependencies= new AtomicInteger();
                    bank.getMessagesOfType(Messages.SendConnection.class).forEach(msg -> {
                        dependencies.addAndGet(1);
                            });
                            bank.numberExposedBanks =dependencies.get();
                            });
    }


    public static Action<Bank> spontaneousDefault(long ID) {
        return Action.create(
               Bank.class, bank -> {
                    if (bank.getID() == ID) {
                        bank.switchToDefault();
                    }
                });
    }


    public static Action<Bank> sendState() {
        return Action.create(
                Bank.class, bank -> {
                    bank.getLinks(Links.AgentLink.class).send(Messages.StateMessage.class, (msg, link) -> {
                        msg.state = bank.getBankState();
                    });
                });
    }


    public static Action<Bank> updateBalanceSheet() {
        return Action.create(
               Bank.class, bank ->{
                    if (bank.state==State.SOLVENT) {
                        AtomicInteger defaultedBansk= new AtomicInteger();
                        bank.getMessagesOfType(Messages.StateMessage.class).forEach(msg -> {
                            if (msg.state == State.DEFAULT) {
                                defaultedBansk.addAndGet(1);
                            }
                        });
                        double percentageDefaultedBanks=defaultedBansk.get()/bank.numberExposedBanks;

                        double k=bank.cashBuffer/bank.percentInterBankAssets;
                        if(percentageDefaultedBanks>(bank.cashBuffer/bank.getGlobals().percentBankLiability)){
                            bank.getLongAccumulator("Default").add(1);
                            bank.switchToDefault();
                        }

                    }
                });
    }


    public static Action<Bank> writeData() {
        return Action.create(
                Bank.class, bank -> {
                    Monitor agentMonitor = new Monitor(bank.getID(), bank.outputLongState(), bank.getGlobals().nodeDegree);//
                    ValueRecord agentOutput = agentMonitor.getMonitorValue();
                    bank.getContext().getChannels().getOutputChannelWriterById("contagion_output").write(agentOutput);
                });
    }


    private void switchToDefault(){
            state = State.DEFAULT;
        }


    private long outputLongState(){
        long state_long=-99;
        if (state == State.SOLVENT) {
            state_long = 0;
        } else if (state == State.DEFAULT){
            state_long = 1;
        }
        return state_long;
    }


    public State getBankState() {
        return state;
    }
}
