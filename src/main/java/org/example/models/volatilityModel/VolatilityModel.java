package org.example.models.volatilityModel;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;

@ModelSettings(macroStep = 1, ticks = 5000)
public class VolatilityModel extends AgentBasedModel<VolatilityModel.Globals> {

  public static final class Globals extends GlobalState {

    // user inputs to toggle between transition strategies
    @Input(name = "Transition Probability Approach")
    boolean TPATransitionStrategy = true;

    @Input(name = "Discreate Choice Approach")
    boolean DCATransitionStrategy = false;

    // user inputs for fine tuning adaption coefficients
    @Input double alpha_w = 1580;
    @Input double alpha_p = 0;
    @Input double alpha_o = 0;
  }

  {
    createDoubleAccumulator("price");
    createDoubleAccumulator("returns");
    createDoubleAccumulator("fundamentalistDemand");
    createDoubleAccumulator("trendFollowerDemand");
    createDoubleAccumulator("ratioOfFundamentalists");
    createDoubleAccumulator("ratioOfChartists");
    createDoubleAccumulator("portfolio_performance_chartists");
    createDoubleAccumulator("portfolio_performance_fundamentalists");
    registerAgentTypes(Trader.class, Market.class);
    registerLinkTypes(Links.MrktToTraderLink.class, Links.TraderToMrktLink.class);
  }

  @Override
  public void setup() {
    // create group of fundamentalist and chartists of equal proportions
    Group<Trader> traders =
        generateGroup(
            Trader.class,
            200,
            trader -> {
              double prob = trader.getPrng().uniform(0, 1).sample();
              trader.behaviour = prob > 0.5 ? Behaviour.FUNDAMENTALIST : Behaviour.CHARTISTS;
            });

    // create one market maker
    Group<Market> market = generateGroup(Market.class, 1);

    // fully connect the market and the traders to each other
    market.fullyConnected(traders, Links.MrktToTraderLink.class);
    traders.fullyConnected(market, Links.TraderToMrktLink.class);

    super.setup();
    getDoubleAccumulator("price").add(1);
    getDoubleAccumulator("ratioOfFundamentalists").add(0.5);
    getDoubleAccumulator("ratioOfChartists").add(0.5);
  }

  @Override
  public void step() {
    super.step();
    run(Market.distributePriceInformation, Trader.receivePriceInformation);
    run(Market.calculatePortfolioPerformance);
    run(Market.calculateFractionOfEachStrategies, Trader.changeStrategies);
    run(Market.calculateAdaptionValue);
    run(Trader.sendDemand, Market.calcTotalDemandAndNewPrice);
  }
}
