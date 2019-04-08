package org.example.models.trading;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.testkit.TestResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TraderTest {

  public static final int TARGET_LINK_ID = 1;
  private TestKit<TradingModel.Globals> testKit;
  private Trader trader;
  private int marketPriceChange = 15;

  @Before
  public void init() {
    testKit = TestKit.create(TradingModel.Globals.class);
    testKit.registerLinkTypes(Links.TradeLink.class);
    trader = testKit.addAgent(Trader.class);
    trader.addLink(TARGET_LINK_ID, Links.TradeLink.class);

    testKit.createLongAccumulator("buys");
    testKit.createLongAccumulator("sells");

    testKit.send(Messages.MarketPriceChange.class, m -> m.setBody(marketPriceChange)).to(trader);
  }

  @Test
  public void should_notProcess_infoSignal_lessThan_tradingThresh() {
    testKit.getGlobals().informationSignal = 0.002;
    trader.tradingThresh = testKit.getGlobals().informationSignal + 0.001;

    TestResult testResult = testKit.testAction(trader, Trader.processInformation());

    assertEquals(testResult.getLongAccumulator("buys").value(), 0);
    assertEquals(testResult.getMessagesOfType(Messages.BuyOrderPlaced.class).size(), 0);

    assertEquals(testResult.getLongAccumulator("sells").value(), 0);
    assertEquals(testResult.getMessagesOfType(Messages.SellOrderPlaced.class).size(), 0);
  }

  @Test
  public void should_buy_positive_infoSignal() {
    testKit.getGlobals().informationSignal = 0.002;
    trader.tradingThresh = testKit.getGlobals().informationSignal - 0.001;

    TestResult testResult = testKit.testAction(trader, Trader.processInformation());

    assertEquals(1, testResult.getLongAccumulator("buys").value());
    assertEquals(1, testResult.getMessagesOfType(Messages.BuyOrderPlaced.class).size());

    assertEquals(0, testResult.getLongAccumulator("sells").value());
    assertEquals(0, testResult.getMessagesOfType(Messages.SellOrderPlaced.class).size());
  }

  @Test
  public void should_sell_negative_infoSignal() {
    testKit.getGlobals().informationSignal = -0.002;
    trader.tradingThresh = testKit.getGlobals().informationSignal - 0.001;

    TestResult testResult = testKit.testAction(trader, Trader.processInformation());

    assertEquals(0, testResult.getLongAccumulator("buys").value());
    assertEquals(0, testResult.getMessagesOfType(Messages.BuyOrderPlaced.class).size());

    assertEquals(1, testResult.getLongAccumulator("sells").value());
    assertEquals(1, testResult.getMessagesOfType(Messages.SellOrderPlaced.class).size());
  }

  @Test
  public void should_updateThreshold_random_lessThan_updateFrequency() {
    trader.random = mock(RandomGenerator.class);
    when(trader.random.nextDouble()).thenReturn(testKit.getGlobals().updateFrequency - 1);

    testKit.testAction(trader, Trader.updateThreshold());

    assertEquals(marketPriceChange, trader.tradingThresh, 0);
  }

  @Test
  public void should_updateThreshold_random_moreThan_updateFrequency() {
    trader.random = mock(RandomGenerator.class);
    when(trader.random.nextDouble()).thenReturn(testKit.getGlobals().updateFrequency + 1);

    testKit.testAction(trader, Trader.updateThreshold());

    assertNotEquals(marketPriceChange, trader.tradingThresh, 0);
  }
}
