package org.example.models.advanced3;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.functions.SerializableConsumer;

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

  private static Action<Bank> action(SerializableConsumer<Bank> consumer) {
    return Action.create(Bank.class, consumer);
  }

  static Action<Bank> processApplication =
      action(
          bank ->
              bank.getMessagesOfType(Messages.MortgageApplication.class)
                  .stream()
                  .filter(m -> m.amount / m.income <= bank.getGlobals().LTILimit)
                  .filter(m -> m.wealth > m.amount * (1 - bank.getGlobals().LTVLimit))
                  .forEach(
                      m -> {
                        int totalAmount =
                            (int) (m.amount * Math.pow(bank.interest(), bank.termInYears));

                        bank.send(
                                Messages.ApplicationSuccessful.class,
                                newMessage -> {
                                  newMessage.amount = totalAmount;
                                  newMessage.termInMonths = bank.termInMonths;
                                  newMessage.repayment = totalAmount / bank.termInMonths;
                                })
                            .to(m.getSender());
                        bank.nbMortgages += 1;
                        bank.assets += m.amount;
                        bank.debt += m.amount;
                      }));

  void accumulateIncome() {
    income = 0;
    getMessagesOfType(Messages.Payment.class).forEach(payment -> income += payment.repayment);

    double NIM = 0.25;
    assets += (income * NIM);
  }

  void processArrears() {
    List<Messages.Arrears> arrears = getMessagesOfType(Messages.Arrears.class);
    // Count bad loans
    calcBadLoans(arrears);
    // Calculate provisions
    double stage1Provisions = calcStageOneProvisions(arrears);
    double stage2Provisions = calcStageTwoProvisions(arrears);
    getGlobals().stage1Provisions = stage1Provisions * 0.01;
    getGlobals().stage2Provisions = stage2Provisions * 0.03;

    writeLoans(arrears);
  }

  private void calcBadLoans(List<Messages.Arrears> arrears) {
    arrears.forEach(
        arrear -> {
          if (arrear.monthsInArrears > 3) {
            getLongAccumulator("badLoans").add(1);
          }
        });
  }

  private void writeLoans(List<Messages.Arrears> arrears) {
    impairments = 0;
    arrears.forEach(
        arrear -> {
          // A mortgage is written off if it is more than 6 months in arrears.
          if (arrear.monthsInArrears > 6) {
            impairments += arrear.outstandingBalance;

            getLongAccumulator("writeOffs").add(1);

            // Notify the sender their loan was defaulted.
            send(Messages.LoanDefault.class).to(arrear.getSender());
          }
        });
    assets -= impairments;
  }

  private int calcStageTwoProvisions(List<Messages.Arrears> arrears) {
    return arrears
        .stream()
        .filter(m -> m.monthsInArrears > 1 && m.monthsInArrears < 3)
        .mapToInt(m -> m.outstandingBalance)
        .sum();
  }

  private int calcStageOneProvisions(List<Messages.Arrears> arrears) {
    return arrears
        .stream()
        .filter(m -> m.monthsInArrears <= 1)
        .mapToInt(m -> m.outstandingBalance)
        .sum();
  }

  /** Remove any mortgages that have closed from the books. */
  void clearPaidMortgages() {
    int balancePaidOff = 0;

    for (Messages.CloseMortgageAmount closeAmount :
        getMessagesOfType(Messages.CloseMortgageAmount.class)) {
      balancePaidOff += closeAmount.getBody();
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
