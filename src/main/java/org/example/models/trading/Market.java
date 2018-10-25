package org.example.models.trading;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.functions.SerializableConsumer;

public class Market extends Agent<TradingModel.Globals> {

  private static double price = 4.0;

  private static Action<Market> action(SerializableConsumer<Market> consumer) {
    return Action.create(Market.class, consumer);
  }

  public static Action<Market> calcPriceImpact() {
    return action(
        market -> {
          int buys = market.getMessagesOfType(Messages.BuyOrderPlaced.class).size();
          int sells = market.getMessagesOfType(Messages.SellOrderPlaced.class).size();

          int netDemand = buys - sells;

          if (netDemand == 0) {
            market.getLinks(Links.TradeLink.class).send(Messages.MarketPriceChange.class, 0);
          } else {
            long nbTraders = market.getGlobals().nbTraders;
            double lambda = market.getGlobals().lambda;
            double priceChange = (netDemand / (double) nbTraders) / lambda;
            price += priceChange;

            market.getDoubleAccumulator("price").add(price);
            market.getLinks(Links.TradeLink.class).send(Messages.MarketPriceChange.class, priceChange);
          }
        });
  }
}
