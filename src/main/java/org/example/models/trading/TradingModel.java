package org.example.models.trading;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;
import simudyne.core.annotations.Variable;
import simudyne.core.graph.DoubleAccumulator;
import simudyne.core.graph.LongAccumulator;
import simudyne.core.rng.SeededRandom;


@ModelSettings(macroStep = 60)
public class TradingModel extends AgentBasedModel<TradingModel.Globals> {

    public SeededRandom rng = SeededRandom.create(42);

    public static final class Globals extends GlobalState {
        @Input(name = "Update Frequency")
        public double updateFrequency = 0.01;

        //@Constant(name = "Number of Traders")
        public long nbTraders = 200;

        @Input(name = "Lambda")
        public double lambda = 10;

        @Input(name = "Volatility of Information Signal")
        public double volatilityInfo = 0.001;

        public double informationSignal;
    }

    {
        createLongAccumulator("buys", "Number of buy orders");
        createLongAccumulator("sells", "Number of sell orders");
        createDoubleAccumulator("price", "Price");

        registerAgentTypes(Market.class, Trader.class);
        registerLinkTypes(Links.MktTraderLink.class, Links.TraderMktLink.class);
    }

    @Override
    public void setup() {
        getGlobals().informationSignal = rng.gaussian(0.0, getGlobals().volatilityInfo).sample();

        Group<Trader> traderGroup =
                generateGroup(Trader.class, getGlobals().nbTraders);
        Group<Market> marketGroup =
                generateGroup(Market.class, 1, market -> {
                    market.price = 4.0;
                });

        traderGroup.fullyConnected(marketGroup, Links.TraderMktLink.class);
        marketGroup.fullyConnected(traderGroup, Links.MktTraderLink.class);

        super.setup();
    }

    @Override
    public void step() {
        super.step();

        getGlobals().informationSignal = rng.gaussian(0.0, getGlobals().volatilityInfo).sample();

        run(
                Trader.processInformation(),
                Market.calcPriceImpact(),
                Trader.updateThreshold()
        );
    }
}
