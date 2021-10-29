package org.example.models.cda;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;

import java.util.Random;

@ModelSettings(macroStep = 1000, ticks = 1000)
public class CDAModel extends AgentBasedModel<CDAModel.Globals> {

    public static final class Globals extends GlobalState {
        @Input(name = "Max Frequency")
        public double frequencyMax = 0.4;

        //@Constant(name = "Number of Traders")
        public long nbTraders = 100;

        @Constant(name = "Vol of Limit Prices")
        public double volLimitPrice = 50.0;

        @Constant(name = "Predictor Seed")
        public long predictSeed = 1234;
    }

    {
        createDoubleAccumulator("transactions", "Market Prices");
        createDoubleAccumulator("Belief", "underlying fundamental value (mu)");
        createDoubleAccumulator("BeliefVol", "underlying fundamental value (vol of vol)");
        createDoubleAccumulator("kurtosis", "Kurtosis (fat-tailness)");
        createDoubleAccumulator("spread", "Bid-Ask Spread");

        registerAgentTypes(Trader.class, Predictor.class, Exchange.class);
        registerLinkTypes(Links.TraderLink.class, Links.ExchangeLink.class, Links.PredictorLink.class);
    }

    @Override
    public void setup() {

        Random random = getGlobals().predictSeed == 0 ? new Random() : new Random(getGlobals().predictSeed);

        Group<Trader> traderGroup = generateGroup(Trader.class, getGlobals().nbTraders, trader -> {
            trader.tradingThreshold = 0.1 + random.nextFloat() * getGlobals().frequencyMax;
        });
        Group<Exchange> marketGroup = generateGroup(Exchange.class, 1);
        Group<Predictor> predictorGroup = generateGroup(Predictor.class, 1, predictor -> {
            predictor.scale = getGlobals().volLimitPrice;
            predictor.random = getGlobals().predictSeed == 0 ? new Random() : new Random(getGlobals().predictSeed);
        });

        traderGroup.fullyConnected(marketGroup, Links.ExchangeLink.class);
        marketGroup.fullyConnected(traderGroup, Links.TraderLink.class);

        traderGroup.fullyConnected(predictorGroup, Links.PredictorLink.class);
        predictorGroup.fullyConnected(traderGroup, Links.PredictorLink.class);

        super.setup();
    }

    @Override
    public void step() {
        super.step();

        run(Predictor.inform(), Trader.updateBelief());
        run(Trader.submitOrder(), Exchange.clear(), Trader.update());
        run(Exchange.update());
    }
}


