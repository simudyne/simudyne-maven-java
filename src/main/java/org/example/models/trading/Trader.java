package org.example.models.trading;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.Random;

public class Trader extends Agent<TradingModel.Globals> {

  static Random random = new Random();

  @Variable double tradingThresh = random.nextGaussian();

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
          if (random.nextDouble() <= updateFrequency) {
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
