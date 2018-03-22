package org.example.models.trading;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

public class Market extends Agent<TradingModel.Globals> {

  private static double price = 4.0;

  public static Action<Market> calcPriceImpact() {
    return Action.create(
        Market.class,
        market -> {
          int buys = market.getMessagesOfType(Messages.BuyOrderPlaced.class).size();
          int sells = market.getMessagesOfType(Messages.SellOrderPlaced.class).size();

          int netDemand = buys - sells;

          if (netDemand == 0) {
            market.broadcastMessage(0);
          } else {
            long nbTraders = market.getGlobals().nbTraders;
            double lambda = market.getGlobals().lambda;
            double priceChange = (netDemand / (double) nbTraders) / lambda;
            price += priceChange;

            market.getDoubleAccumulator("price").add(price);
            market.broadcastMessage(priceChange);
          }
        });
  }
}
