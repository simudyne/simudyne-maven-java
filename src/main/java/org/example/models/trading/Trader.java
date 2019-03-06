package org.example.models.trading;

import org.apache.commons.math3.random.RandomGenerator;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class Trader extends Agent<TradingModel.Globals> {

  RandomGenerator random;
  @Variable double tradingThresh;

  @Override
  public void init() {
    random = this.getPrng().generator;
    tradingThresh = random.nextGaussian();
  }

  private static Action<Trader> action(SerializableConsumer<Trader> consumer) {
    return Action.create(Trader.class, consumer);
  }

  public static Action<Trader> processInformation() {
    return action(
        trader -> {
          double informationSignal = trader.getGlobals().informationSignal;

          if (Math.abs(informationSignal) > trader.tradingThresh) {
            if (informationSignal > 0) {
              trader.buy();
            } else {
              trader.sell();
            }
          }
        });
  }

  public static Action<Trader> updateThreshold() {
    return action(
        trader -> {
          double updateFrequency = trader.getGlobals().updateFrequency;
          if (trader.random.nextDouble() <= updateFrequency) {
            trader.tradingThresh =
                trader.getMessageOfType(Messages.MarketPriceChange.class).getBody();
          }
        });
  }

  private void buy() {
    getLongAccumulator("buys").add(1);
    getLinks(Links.TradeLink.class).send(Messages.BuyOrderPlaced.class);
  }

  private void sell() {
    getLongAccumulator("sells").add(1);
    getLinks(Links.TradeLink.class).send(Messages.SellOrderPlaced.class);
  }
}
