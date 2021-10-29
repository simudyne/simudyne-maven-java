package mortgage;

import org.example.models.mortgage.Messages;
import org.example.models.mortgage.MortgageModel;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.List;

public class Bank extends Agent<MortgageModel.Globals> {

  private int debt = 0;
  private int termInYears = 25;

  private double interest() {
    return 1 + (getGlobals().interestRate / 100);
  }

  private int termInMonths = termInYears * 12;

  private int assets = 1000000000;
  private int impairments = 0;
  private int nbMortgages = 0;
  private int income = 0;
  private int balancePaidOff = 0;

  @Variable
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
                                          m -> m.amount / m.income <= bank.getGlobals().LTILimit)
                                  .filter(
                                          m ->
                                                  m.wealth
                                                          > m.amount * (1 - bank.getGlobals().LTVLimit))
                                  .forEach(
                                          m -> {
                                            int totalAmount =
                                                    (int)
                                                            (m.amount * Math.pow(bank.interest(), bank.termInYears));

                                            bank.send(Messages.ApplicationSuccessful.class, application -> {
                                              application.amount = totalAmount;
                                              application.repayment = totalAmount / bank.termInMonths;
                                              application.termInMonths = bank.termInMonths;
                                            }).to(m.getSender());

                                            bank.nbMortgages += 1;
                                            bank.assets += m.amount;
                                            bank.debt += m.amount;
                                          }));

  public void accumulateIncome() {
    income = 0;

    List<Messages.Payment> goodLoans = getMessagesOfType(Messages.Payment.class);
    goodLoans.forEach(payment -> income += payment.repayment);

    double stage1Provisions =
            goodLoans
                    .stream()
                    .mapToDouble(m -> m.outstandingBalance * (1.0 / m.creditScore))
                    .sum();

    getGlobals().stage1Provisions = stage1Provisions;

    double NIM = 0.25;
    assets += (income * NIM);
  }

  public void processArrears() {
    impairments = 0;
    getMessagesOfType(Messages.Arrears.class).forEach(
            arrears -> {
              if (arrears.monthsInArrears > 3) {
                getLongAccumulator("badLoans").add(1);
              }
              if (arrears.monthsInArrears > 6) {
                impairments += arrears.outstandingBalance;
                getLongAccumulator("writeOffs").add(1);
                // Notify the sender their loan was defaulted.
                send(Messages.Default.class, true).to(arrears.getSender());
              }
            });

    // Calculate provisions
    double stage2Provisions = getMessagesOfType(Messages.Arrears.class)
            .stream()
            .filter(m -> m.monthsInArrears > 1 && m.monthsInArrears < 3)
            .mapToDouble(m -> m.outstandingBalance * (1.0 / m.creditScore))
            .sum();
    getGlobals().stage2Provisions = stage2Provisions;

    assets -= impairments;
  }

  /**
   * Remove any mortgages that have closed from the books.
   */

  public void clearPaidMortgages() {

    getMessagesOfType(Messages.CloseMortgage.class).forEach(
            closeMortgage -> {
              balancePaidOff += closeMortgage.amount;
              nbMortgages -= 1;

            });
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
