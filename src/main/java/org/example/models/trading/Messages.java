package org.example.models.trading;

import simudyne.core.graph.Message;

public class Messages {
  public static class BuyOrderPlaced extends Message.Empty {}

  public static class SellOrderPlaced extends Message.Empty {}

  public static class MarketPriceChange extends Message.Double {}
}
