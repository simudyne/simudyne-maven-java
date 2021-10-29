package org.example.models.GaiKapadia;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.Sequence;
import simudyne.core.annotations.Input;
import simudyne.core.graph.LongAccumulator;
import simudyne.core.rng.SeededRandom;


public class GaiKapadiaModel  extends AgentBasedModel<GaiKapadiaModel.Globals> {

    @Input(name = "Number of Agents")
    public int numAgents = 100;

    public static final class Globals extends GlobalState {
        public int ticks = 0;

        @Input(name = "NumberBanks")
        public int numberBanks = 100;

        @Input(name = "NodeDegree")
        public double nodeDegree = 6;

        @Input(name = "CashBuffer")
        public double cashBuffer = 0.05;

        @Input(name = "PercentBankLiability")
        public double percentBankLiability = 0.2;

    }

    long startTime;
    boolean done = false;
    LongAccumulator infectedAcc;

    @Override
    public void init() {
        startTime = System.currentTimeMillis();
        registerAgentTypes(Bank.class);
        registerLinkTypes(Links.AgentLink.class);
        registerMessageTypes(Messages.SendConnection.class, Messages.StateMessage.class);
        infectedAcc = createLongAccumulator("Default");

        getContext()
                .getChannels()
                .createOutputChannel()
                .setId("contagion_output")
                .setSchema(Monitor.getMonitorSchema())
                .addLabel("simudyne:parquet")
                .build();
    }



    @Override
    public void setup() {
        getGlobals().numberBanks = numAgents;
        Group<Bank> bankGroup = generateGroup(Bank.class, getGlobals().numberBanks, banks -> {
                    banks.cashBuffer = getGlobals().cashBuffer;
                    banks.percentInterBankAssets = getGlobals().percentBankLiability;
            });
        bankGroup.fullyConnected(bankGroup, Links.AgentLink.class);
        super.setup();
    }

    @Override
    public void step() {
        super.step();
        getGlobals().ticks++;
        if (getContext().getTick() == 0) {
            run(Sequence.create(Bank.informNeighbors(), Bank.pruneConnection(getGlobals().nodeDegree)));
            run(Sequence.create(Bank.informNeighbors(), Bank.receiveNeighborInformation()));
            this.getContext().getPrng().generator.setSeed(System.currentTimeMillis());
            SeededRandom pnrg = this.getContext().getPrng();
            run(Sequence.create(Bank.spontaneousDefault(pnrg.generator.nextInt(getGlobals().numberBanks))));
            return;
        }
        run(Sequence.create(Bank.sendState(), Bank.updateBalanceSheet()));

        if (infectedAcc.value() == 0) {
            if (!done) {
                run(Sequence.create(Bank.writeData()));
                done = true;
            }
        }

    }
}

