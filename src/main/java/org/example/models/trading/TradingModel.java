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

import java.util.Random;

@ModelSettings(macroStep = 100)
public class TradingModel extends AgentBasedModel<TradingModel.Globals> {

  public static final class Globals extends GlobalState {
    @Input(name = "Update Frequency")
    public double updateFrequency = 0.01;

    @Constant(name = "Number of Traders")
    public long nbTraders = 1000;

    @Input(name = "Lambda")
    public double lambda = 10;

    @Input(name = "Volatility of Information Signal")
    public double volatilityInfo = 0.001;

    public double informationSignal = new Random().nextGaussian() * volatilityInfo;
  }

  @Variable(name = "Number of buy orders")
  public LongAccumulator buys = createLongAccumulator("buys");

  @Variable(name = "Number of sell orders")
  public LongAccumulator sells = createLongAccumulator("sells");

  @Variable(name = "Price")
  public DoubleAccumulator priceAccumulator = createDoubleAccumulator("price");

  @Override
  public void setup() {
    Group<Trader> traderGroup = generateGroup(Trader.class, getGlobals().nbTraders);
    Group<Market> marketGroup = generateGroup(Market.class, 1);

    traderGroup.fullyConnected(marketGroup);
    marketGroup.fullyConnected(traderGroup);

    super.setup();
  }

  @Override
  public void step() {
    super.step();

    getGlobals().informationSignal = new Random().nextGaussian() * getGlobals().volatilityInfo;

    run(Trader.processInformation(), Market.calcPriceImpact(), Trader.updateThreshold());
  }
}
