package org.example.models.advanced3;

import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.Action;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.testkit.TestResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BankTest {

  public static final double LTI_LIMIT = 4.5;
  public static final double LTV_LIMIT = 0.95;
  private TestKit<MortgageModel.Globals> testKit;
  private Bank bank;

  @Before
  public void init() {
    testKit = TestKit.create(MortgageModel.Globals.class);
    testKit.getGlobals().LTILimit = LTI_LIMIT;
    testKit.getGlobals().LTVLimit = LTV_LIMIT;
    testKit.createLongAccumulator("badLoans");
    testKit.createLongAccumulator("writeOffs");
    testKit.createLongAccumulator("debt");
    testKit.createLongAccumulator("impairments");
    testKit.createLongAccumulator("mortgages");
    testKit.createLongAccumulator("income");
    testKit.createLongAccumulator("assets");
    testKit.createLongAccumulator("equity");
    bank = testKit.addAgent(Bank.class);
  }

  @Test
  public void shouldAcceptMortgageApplication_valid_LTI_andLTVL() {
    int initialMortgages = bank.nbMortgages;
    int initialAssets = bank.assets;
    int initialDebt = bank.debt;
    int mortgageAmount = 100;

    testKit
        .send(
            Messages.MortgageApplication.class,
            m -> {
              m.amount = mortgageAmount;
              // set an income that results in the LTI being higher than the globals LTI
              m.income = (int) ((m.amount / LTI_LIMIT) + 1);
              // set a wealth that results in the LTV being higher than the globals LTV
              m.wealth = (int) ((m.amount * (1 - LTV_LIMIT) + 1));
            })
        .to(bank);

    TestResult testResult = testKit.testAction(bank, Bank.processApplication);
    assertEquals(1, testResult.getMessagesOfType(Messages.ApplicationSuccessful.class).size());

    assertEquals(initialMortgages + 1, bank.nbMortgages);
    assertEquals(initialAssets + mortgageAmount, bank.assets);
    assertEquals(initialDebt + mortgageAmount, bank.debt);
  }

  @Test
  public void shouldRejectMortgageApplication_invalid_LTI() {
    testKit
        .send(
            Messages.MortgageApplication.class,
            m -> {
              m.amount = 100;
              // set an income that results in the LTI being lower than the globals LTI
              m.income = (int) ((m.amount / LTI_LIMIT) - 1);
            })
        .to(bank);

    TestResult testResult = testKit.testAction(bank, Bank.processApplication);
    assertTrue(testResult.getMessagesOfType(Messages.ApplicationSuccessful.class).isEmpty());
  }

  @Test
  public void shouldRejectMortgageApplication_invalid_LTV() {
    testKit
        .send(
            Messages.MortgageApplication.class,
            m -> {
              m.amount = 100;
              m.income = 100;
              // set a wealth that results in the LTV being lower than the globals LTV
              m.wealth = (int) ((m.amount * (1 - LTV_LIMIT) - 1));
            })
        .to(bank);

    TestResult testResult = testKit.testAction(bank, Bank.processApplication);
    assertTrue(testResult.getMessagesOfType(Messages.ApplicationSuccessful.class).isEmpty());
  }

  @Test
  public void shouldAccumulateIncome() {
    int initialAssets = bank.assets;
    int repayment1 = 103;
    int repayment2 = 108;
    testKit.send(Messages.Payment.class, p -> p.repayment = repayment1).to(bank);
    testKit.send(Messages.Payment.class, p -> p.repayment = repayment2).to(bank);

    testKit.testAction(bank, Action.create(Bank.class, Bank::accumulateIncome));

    assertEquals(initialAssets + ((repayment1 + repayment2) * bank.NIM), bank.assets, 1);
  }

  @Test
  public void shouldProcessStage1Arrears() {
    int initialAssets = bank.assets;
    int outstandingBalance1 = 34;
    int outstandingBalance2 = 56;
    sendArrearsMessage(outstandingBalance1, 1);
    sendArrearsMessage(outstandingBalance2, 1);

    TestResult testResult =
        testKit.testAction(bank, Action.create(Bank.class, Bank::processArrears));

    assertEquals(0, testResult.getLongAccumulator("badLoans").value());
    assertEquals(0, testResult.getLongAccumulator("writeOffs").value());
    assertEquals(0, bank.stage2Provisions, 0);
    assertEquals((outstandingBalance1 + outstandingBalance2) * 0.01, bank.stage1Provisions, 0);
    assertEquals(0, testResult.getMessagesOfType(Messages.LoanDefault.class).size());
    assertEquals(initialAssets, bank.assets);
  }

  @Test
  public void shouldProcessStage2Arrears() {
    int initialAssets = bank.assets;
    int outstandingBalance1 = 34;
    int outstandingBalance2 = 56;
    sendArrearsMessage(outstandingBalance1, 2);
    sendArrearsMessage(outstandingBalance2, 2);

    TestResult testResult =
        testKit.testAction(bank, Action.create(Bank.class, Bank::processArrears));

    assertEquals(0, testResult.getLongAccumulator("badLoans").value());
    assertEquals(0, testResult.getLongAccumulator("writeOffs").value());
    assertEquals(0, bank.stage1Provisions, 0);
    assertEquals((outstandingBalance1 + outstandingBalance2) * 0.03, bank.stage2Provisions, 0);
    assertEquals(0, testResult.getMessagesOfType(Messages.LoanDefault.class).size());
    assertEquals(initialAssets, bank.assets);
  }

  @Test
  public void shouldProcessBadLoans() {
    int initialAssets = bank.assets;
    int outstandingBalance1 = 34;
    int outstandingBalance2 = 56;
    sendArrearsMessage(outstandingBalance1, 4);
    sendArrearsMessage(outstandingBalance2, 4);

    TestResult testResult =
        testKit.testAction(bank, Action.create(Bank.class, Bank::processArrears));

    assertEquals(2, testResult.getLongAccumulator("badLoans").value());
    assertEquals(0, testResult.getLongAccumulator("writeOffs").value());
    assertEquals(0, bank.stage1Provisions, 0);
    assertEquals(0, bank.stage2Provisions, 0);
    assertEquals(0, testResult.getMessagesOfType(Messages.LoanDefault.class).size());
    assertEquals(initialAssets, bank.assets);
  }

  @Test
  public void shouldProcessWriteOffs() {
    int initialAssets = bank.assets;
    int outstandingBalance1 = 34;
    int outstandingBalance2 = 56;
    sendArrearsMessage(outstandingBalance1, 7);
    sendArrearsMessage(outstandingBalance2, 7);

    TestResult testResult =
        testKit.testAction(bank, Action.create(Bank.class, Bank::processArrears));

    assertEquals(2, testResult.getLongAccumulator("badLoans").value());
    assertEquals(2, testResult.getLongAccumulator("writeOffs").value());
    assertEquals(0, bank.stage1Provisions, 0);
    assertEquals(0, bank.stage2Provisions, 0);
    assertEquals(2, testResult.getMessagesOfType(Messages.LoanDefault.class).size());
    assertEquals(initialAssets - outstandingBalance1 - outstandingBalance2, bank.assets);
  }

  private void sendArrearsMessage(int outstandingBalance, int monthsInArrears) {
    testKit
        .send(
            Messages.Arrears.class,
            m -> {
              m.monthsInArrears = monthsInArrears;
              m.outstandingBalance = outstandingBalance;
            })
        .to(bank);
  }

  @Test
  public void shouldProcessPaidMortgages() {
    int initialDebt = bank.debt;
    int initialAssets = bank.assets;
    int initialNbMortgages = bank.nbMortgages;

    int mortgageAmount1 = 59;
    int mortgageAmount2 = 32;
    testKit
        .send(Messages.CloseMortgageAmount.class, amount -> amount.setBody(mortgageAmount1))
        .to(bank);
    testKit
        .send(Messages.CloseMortgageAmount.class, amount -> amount.setBody(mortgageAmount2))
        .to(bank);

    testKit.testAction(bank, Action.create(Bank.class, Bank::clearPaidMortgages));

    assertEquals(initialDebt - mortgageAmount1 - mortgageAmount2, bank.debt);
    assertEquals(initialAssets - mortgageAmount1 - mortgageAmount2, bank.assets);
    assertEquals(initialNbMortgages - 2, bank.nbMortgages);
  }

  @Test
  public void shouldUpdateAccumulators() {
    bank.debt = 98;
    bank.impairments = 23;
    bank.income = 90;
    bank.assets = 98765;
    bank.nbMortgages = 8;

    TestResult testResult =
        testKit.testAction(bank, Action.create(Bank.class, Bank::updateAccumulators));

    assertEquals(bank.debt, testResult.getLongAccumulator("debt").value());
    assertEquals(bank.impairments, testResult.getLongAccumulator("impairments").value());
    assertEquals(bank.nbMortgages, testResult.getLongAccumulator("mortgages").value());
    assertEquals(bank.income, testResult.getLongAccumulator("income").value());
    assertEquals(bank.assets, testResult.getLongAccumulator("assets").value());
    assertEquals(bank.assets - bank.debt, testResult.getLongAccumulator("equity").value());
  }
}
