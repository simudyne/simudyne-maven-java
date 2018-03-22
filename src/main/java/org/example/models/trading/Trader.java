package org.example.models.trading;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.Random;

public class Trader extends Agent<TradingModel.Globals> {

  static Random random = new Random();

  @Variable double tradingThresh = random.nextGaussian();

  public static Action<Trader> processInformation() {
    return Action.create(
        Trader.class,
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
    return Action.create(
        Trader.class,
        trader -> {
          double updateFrequency = trader.getGlobals().updateFrequency;
          if (random.nextDouble() <= updateFrequency) {
            trader.tradingThresh = trader.getMessageOfType(Double.class).getBody();
          }
        });
  }

  private void buy() {
    getLongAccumulator("buys").add(1);
    broadcastMessage(new Messages.BuyOrderPlaced());
  }

  private void sell() {
    getLongAccumulator("sells").add(1);
    broadcastMessage(new Messages.SellOrderPlaced());
  }
}
