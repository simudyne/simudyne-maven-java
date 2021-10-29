package org.example.models.volatilityModel;

import simudyne.core.graph.Message;

public class Messages {

  public static class DemandMessage extends Message {
    public double demand;
    public Behaviour behaviour;
  }

  public static class RatioOfFundementals extends Message.Double {}

  public static class PriceInfo extends Message {
    double price[];
  }
}
