package org.example.models.advanced3;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.graph.Message;

import java.util.List;

public class Bank extends Agent<MortgageModel.Globals> {
  private int debt = 0;

  private int termInYears = 15; // Should be 25

  private double interest() {
    return 1 + (getGlobals().interestRate / 100);
  }

  private int termInMonths = termInYears * 12;
  private int assets = 10000000;
  private int impairments = 0;
  private int nbMortgages = 0;
  private int income = 0;

  private int equity() {
    return assets - debt;
  }

  public static Action<Bank> processApplication =
      new Action<>(
          Bank.class,
          bank ->
              bank.getMessagesOfType(Messages.MortgageApplication.class)
                  .stream()
                  .filter(
                      m -> m.getBody().amount / m.getBody().income <= bank.getGlobals().LTILimit)
                  .filter(
                      m ->
                          m.getBody().wealth
                              > m.getBody().amount * (1 - bank.getGlobals().LTVLimit))
                  .forEach(
                      m -> {
                        int totalAmount =
                            (int)
                                (m.getBody().amount * Math.pow(bank.interest(), bank.termInYears));

                        bank.sendMessage(
                            new Messages.ApplicationSuccessful(
                                totalAmount, bank.termInMonths, totalAmount / bank.termInMonths),
                            m.getSender());
                        bank.nbMortgages += 1;
                        bank.assets += m.getBody().amount;
                        bank.debt += m.getBody().amount;
                      }));

  public void accumulateIncome() {
    income = 0;

    getMessagesOfType(Messages.Payment.class)
        .forEach(payment -> income += payment.getBody().repayment);

    double NIM = 0.25;
    assets += (income * NIM);
  }

  public void processArrears() {
    List<Message<Messages.Arrears>> arrears = getMessagesOfType(Messages.Arrears.class);

    // Count bad loans

    arrears.forEach(
        arrear -> {
          if (arrear.getBody().monthsInArrears > 3) {
            getLongAccumulator("badLoans").add(1);
          }
        });

    // Calculate provisions

    double stage1Provisions =
        arrears
            .stream()
            .filter(m -> m.getBody().monthsInArrears <= 1)
            .mapToInt(m -> m.getBody().outstandingBalance)
            .sum();

    double stage2Provisions =
        arrears
            .stream()
            .filter(m -> m.getBody().monthsInArrears > 1 && m.getBody().monthsInArrears < 3)
            .mapToInt(m -> m.getBody().outstandingBalance)
            .sum();

    getGlobals().stage1Provisions = stage1Provisions * 0.01;
    getGlobals().stage2Provisions = stage2Provisions * 0.03;

    // Write off loans and calculate impairments

    impairments = 0;

    arrears.forEach(
        arrear -> {
          // A mortgage is written off if it is more than 6 months in arrears.
          if (arrear.getBody().monthsInArrears > 6) {
            impairments += arrear.getBody().outstandingBalance;

            getLongAccumulator("writeOffs").add(1);

            // Notify the sender their loan was defaulted.
            sendMessage(true, arrear.getSender());
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
