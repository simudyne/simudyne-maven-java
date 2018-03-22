package org.example.models.advanced2;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Variable;
import simudyne.core.graph.Message;

public class Bank extends Agent<GlobalState> {
  @Variable int debt = 0;
  @Variable int assets = 10000000;

  @Variable
  int equity() {
    return assets - debt;
  }

  @Variable private int nbMortgages = 0;

  private int termInYears = 15; // Should be 25
  private double interest = 1.05;
  private int termInMonths = termInYears * 12;

  private int impairments = 0;
  private int income = 0;

  private double LTILimit = 4.5;
  private double LTVLimit = 0.95;

  public static Action<Bank> processApplication() {
    return new Action<>(
        Bank.class,
        bank ->
            bank.getMessagesOfType(Messages.MortgageApplication.class)
                .stream()
                .filter(m -> m.getBody().amount / m.getBody().income <= bank.LTILimit)
                .filter(m -> m.getBody().wealth > m.getBody().amount * (1 - bank.LTVLimit))
                .forEach(
                    m -> {
                      bank.sendMessage(
                          new Messages.ApplicationSuccessful(
                              m.getBody().amount,
                              bank.termInMonths,
                              (int) ((m.getBody().amount * bank.interest) / bank.termInMonths)),
                          m.getSender());

                      bank.nbMortgages += 1;
                      bank.assets += m.getBody().amount;
                      bank.debt += m.getBody().amount;
                    }));
  }

  public void accumulateIncome() {
    income = 0;

    getMessagesOfType(Messages.Payment.class)
        .forEach(payment -> income += payment.getBody().repayment);

    double NIM = 0.25;
    assets += (income * NIM);
  }

  /**
   * Record how many bad loans the bank has. A loan is counted as bad if it has been more than 3
   * months in arrears.
   */
  public void processArrears() {
    getMessagesOfType(Messages.Arrears.class)
        .forEach(
            arrears -> {
              if (arrears.getBody().monthsInArrears > 3) {
                getLongAccumulator("badLoans").add(1);
              }
            });
  }

  /** Calculate impairments from written off loans. */
  public void calculateImpairments() {
    impairments = 0;

    getMessagesOfType(Messages.Arrears.class)
        .forEach(
            arrears -> {
              // A mortgage is written off if it is more than 6 months in arrears.
              if (arrears.getBody().monthsInArrears > 6) {
                impairments += arrears.getBody().outstandingBalance;

                getLongAccumulator("writeOffs").add(1);
              }
            });

    // Remove any impairments from our assets total.
    // Note that the debt from the written off loan remains.
    assets -= impairments;
  }

  /** Remove any mortgages that have closed from the books. */
  public void clearPaidMortgages() {
    int balancePaidOff = 0;

    for (Message<Messages.CloseMortgage> close : getMessagesOfType(Messages.CloseMortgage.class)) {
      balancePaidOff += close.getBody().amount;
      nbMortgages -= 1;
    }

    debt -= balancePaidOff;
    assets -= balancePaidOff;
  }

  public void updateAccumulators() {
    getLongAccumulator("debt").add(debt);
    getLongAccumulator("impairments").add(impairments);
    getLongAccumulator("mortgages").add(nbMortgages);
    getLongAccumulator("income").add(income);
    getLongAccumulator("assets").add(assets);
    getLongAccumulator("equity").add(equity());
  }
}
