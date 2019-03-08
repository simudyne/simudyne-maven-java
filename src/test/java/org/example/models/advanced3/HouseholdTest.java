package org.example.models.advanced3;

import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.Action;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.testkit.TestResult;
import simudyne.core.rng.SeededRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HouseholdTest {

  private static final int TARGET_BANK_LINK_ID = 23;
  private TestKit<MortgageModel.Globals> testKit;
  private Household household;
  private Household.Mortgage dummyMortgage = new Household.Mortgage(1, 1, 1, 1);

  @Before
  public void init() {
    testKit = TestKit.create(MortgageModel.Globals.class);
    household = testKit.addAgent(Household.class);
    household.addLink(TARGET_BANK_LINK_ID, Links.BankLink.class);
  }

  @Test
  public void applyForMortgage_doesNothing_whenMortgageExists() {
    household.mortgage = dummyMortgage;

    TestResult result = testKit.testAction(household, Household.applyForMortgage);

    assertFalse(result.getMessageIterator().hasNext());
  }

  @Test
  public void applyForMortgage_sendsApplication() {
    household.mortgage = null;
    SeededRandom prngMock = mock(SeededRandom.class);
    UniformIntegerDistribution mockDist = mock(UniformIntegerDistribution.class);
    household.prng = prngMock;

    when(prngMock.discrete(1, 5)).thenReturn(mockDist);
    when(mockDist.sample()).thenReturn(1);

    TestResult testResult = testKit.testAction(household, Household.applyForMortgage);
    Messages.MortgageApplication messageResult =
        testResult.getMessagesOfType(Messages.MortgageApplication.class).get(0);

    assertEquals(100000 + household.income * 2, messageResult.amount);
    assertEquals(household.income, messageResult.income);
    assertEquals(household.wealth, messageResult.wealth);
  }

  @Test
  public void takeOutMortgage_savesMortgageLocally_when_receivesApplicationSuccessfulMessage() {
    int mortgageAmount = 23;
    int mortgageTerm = 34;
    int mortgageRepayment = 1;
    testKit
        .send(
            Messages.ApplicationSuccessful.class,
            m -> {
              m.amount = mortgageAmount;
              m.termInMonths = mortgageTerm;
              m.repayment = mortgageRepayment;
            })
        .to(household);

    testKit.testAction(household, Household.takeOutMortgage);
    Household.Mortgage expectedMortgage =
        new Household.Mortgage(mortgageAmount, mortgageAmount, mortgageTerm, mortgageRepayment);
    assertEquals(expectedMortgage, household.mortgage);
  }

  @Test
  public void takeOutMortgage_doesNothing_when_receivesNoMessage() {
    testKit.testAction(household, Household.takeOutMortgage);
    assertNull(household.mortgage);
  }

  @Test
  public void writeOff_setsMortgageToNull() {
    household.mortgage = dummyMortgage;
    testKit.send(Messages.LoanDefault.class).to(household);

    testKit.testAction(household, Action.create(Household.class, Household::writeOff));

    assertNull(household.mortgage);
  }

  @Test
  public void writeOff_doesNothing_whenReceivesNoMessages() {
    household.mortgage = dummyMortgage;
    testKit.testAction(household, Action.create(Household.class, Household::writeOff));

    assertEquals(dummyMortgage, household.mortgage);
  }

  @Test
  public void payMortgage_whenMortgageMatured() {
    int mortgageAmount = 34;
    int balanceOutstanding = 12;
    int mortgageRepayment = 12;
    int initialWealth = 100;
    household.mortgage =
        new Household.Mortgage(mortgageAmount, balanceOutstanding, 1, mortgageRepayment);
    household.wealth = initialWealth;

    TestResult testResult =
        testKit.testAction(household, Action.create(Household.class, Household::payMortgage));

    assertEquals(initialWealth - mortgageRepayment, household.wealth);
    assertEquals(0, household.monthsInArrears);
    assertNull(household.mortgage);

    Messages.CloseMortgageAmount messageResult =
        testResult.getMessagesOfType(Messages.CloseMortgageAmount.class).get(0);
    assertEquals(mortgageAmount, messageResult.getBody());
  }

  @Test
  public void payMortgage_whenMortgageNOTMatured() {
    int mortgageAmount = 34;
    int balanceOutstanding = 24;
    int mortgageRepayment = 12;
    int initialWealth = 100;
    household.mortgage =
        new Household.Mortgage(mortgageAmount, balanceOutstanding, 2, mortgageRepayment);
    household.wealth = initialWealth;

    TestResult testResult =
        testKit.testAction(household, Action.create(Household.class, Household::payMortgage));

    assertEquals(initialWealth - mortgageRepayment, household.wealth);
    assertEquals(0, household.monthsInArrears);
    assertEquals(1, household.mortgage.term);
    assertEquals(balanceOutstanding - mortgageRepayment, household.mortgage.balanceOutstanding);

    Messages.Payment messageResult = testResult.getMessagesOfType(Messages.Payment.class).get(0);
    assertEquals(mortgageAmount, messageResult.amount);
    assertEquals(mortgageRepayment, messageResult.repayment);
  }

  @Test
  public void payMortgage_whenInArrears() {
    int mortgageAmount = 34;
    int balanceOutstanding = 24;
    int mortgageRepayment = 12;
    int initialWealth = 1;
    household.mortgage =
        new Household.Mortgage(mortgageAmount, balanceOutstanding, 2, mortgageRepayment);
    household.wealth = initialWealth;

    TestResult testResult =
        testKit.testAction(household, Action.create(Household.class, Household::payMortgage));

    Messages.Arrears messageResult = testResult.getMessagesOfType(Messages.Arrears.class).get(0);
    assertEquals(household.monthsInArrears, messageResult.monthsInArrears);
    assertEquals(household.mortgage.balanceOutstanding, messageResult.outstandingBalance);
  }
}
