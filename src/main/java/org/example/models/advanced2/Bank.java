package org.example.models.advanced2;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class Bank extends Agent<GlobalState> {
  @Variable private int debt = 0;
  @Variable private int assets = 10000000;

  @Variable
  private int equity() {
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

  private static Action<Bank> action(SerializableConsumer<Bank> consumer) {
    return Action.create(Bank.class, consumer);
  }

  static Action<Bank> processApplication() {
    return action(
        bank ->
            bank.getMessagesOfType(Messages.MortgageApplication.class)
                .stream()
                .filter(m -> m.amount / m.income <= bank.LTILimit)
                .filter(m -> m.wealth > m.amount * (1 - bank.LTVLimit))
                .forEach(
                    m -> {
                      bank.send(
                              Messages.ApplicationSuccessful.class,
                              newMessage -> {
                                newMessage.amount = m.amount;
                                newMessage.termInMonths = bank.termInMonths;
                                newMessage.repayment =
                                    (int) ((m.amount * bank.interest) / bank.termInMonths);
                              })
                          .to(m.getSender());
                      bank.nbMortgages += 1;
                      bank.assets += m.amount;
                      bank.debt += m.amount;
                    }));
  }

  void accumulateIncome() {
    income = 0;

    getMessagesOfType(Messages.Payment.class).forEach(payment -> income += payment.repayment);

    double NIM = 0.25;
    assets += (income * NIM);
  }

  /**
   * Record how many bad loans the bank has. A loan is counted as bad if it has been more than 3
   * months in arrears.
   */
  void processArrears() {
    getMessagesOfType(Messages.Arrears.class)
        .forEach(
            arrears -> {
              if (arrears.monthsInArrears > 3) {
                getLongAccumulator("badLoans").add(1);
              }
            });
  }

  /** Calculate impairments from written off loans. */
  void calculateImpairments() {
    impairments = 0;

    getMessagesOfType(Messages.Arrears.class)
        .forEach(
            arrears -> {
              // A mortgage is written off if it is more than 6 months in arrears.
              if (arrears.monthsInArrears > 6) {
                impairments += arrears.outstandingBalance;

                getLongAccumulator("writeOffs").add(1);
              }
            });

    // Remove any impairments from our assets total.
    // Note that the debt from the written off loan remains.
    assets -= impairments;
  }

  /** Remove any mortgages that have closed from the books. */
  void clearPaidMortgages() {
    int balancePaidOff = 0;

    for (Messages.MortgageCloseAmount close :
        getMessagesOfType(Messages.MortgageCloseAmount.class)) {
      balancePaidOff += close.getBody();
      nbMortgages -= 1;
    }

    debt -= balancePaidOff;
    assets -= balancePaidOff;
  }

  void updateAccumulators() {
    getLongAccumulator("debt").add(debt);
    getLongAccumulator("impairments").add(impairments);
    getLongAccumulator("mortgages").add(nbMortgages);
    getLongAccumulator("income").add(income);
    getLongAccumulator("assets").add(assets);
    getLongAccumulator("equity").add(equity());
  }
}
