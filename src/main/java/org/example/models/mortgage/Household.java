package org.example.models.mortgage;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

public class Household extends Agent<MortgageModel.Globals> {

  @Variable
  int income;

  int wealth = 1000;
  int creditScore = 100;

  int repayment() {
    if (mortgage == null) {
      return 0;
    }
    return mortgage.repayment;
  }

  int taxBill = 0;
  Mortgage mortgage;
  int monthsInArrears = 0;

  public void earnIncome() {
    wealth += income / 12;
  }

  public void payMortgage() {
    if (mortgage != null) {
      if (canPay()) {
        wealth -= mortgage.repayment;
        monthsInArrears = 0;
        mortgage.term -= 1;
        mortgage.balanceOutstanding -= mortgage.repayment;
        checkMaturity();
      } else {
        creditScore = 30;
        monthsInArrears += 1;
        getLinks(Links.HHBankLink.class).send(Messages.Arrears.class, (arrears, link) -> {
          arrears.monthsInArrears = monthsInArrears;
          arrears.outstandingBalance = mortgage.balanceOutstanding;
          arrears.creditScore = creditScore;
        });
      }
    }
  }

  private void checkMaturity() {
    if (mortgage.term == 0) {
      getLinks(Links.HHBankLink.class).send(Messages.CloseMortgage.class, (closeMortgage, link) -> {
        closeMortgage.amount = mortgage.amount;
      });
      mortgage = null;
    } else {
      getLinks(Links.HHBankLink.class).send(Messages.Payment.class, (payment, link) -> {
        payment.repayment = mortgage.repayment;
        payment.amount = mortgage.amount;
        payment.outstandingBalance = mortgage.balanceOutstanding;
        payment.creditScore = creditScore;
      });
    }
  }

  private Boolean canPay() {
    return wealth >= mortgage.repayment;
  }

  public static Action<Household> applyForMortgage =
          Action.create(
                  Household.class,
                  h -> {
                    if (h.mortgage == null && h.creditScore > 50) {
                      int purchasePrice = 100000 + h.income * 2;
                      h.getLinks(Links.HHBankLink.class).send(Messages.MortgageApplication.class, (mortgageApplication, link) -> {
                        mortgageApplication.amount = purchasePrice;
                        mortgageApplication.income = h.income;
                        mortgageApplication.wealth = h.wealth;
                      });
                    }
                  });

  public static Action<Household> takeOutMortgage =
          Action.create(
                  Household.class,
                  h ->
                          h.hasMessageOfType(
                                  Messages.ApplicationSuccessful.class,
                                  message ->
                                          h.mortgage =
                                                  new Mortgage(
                                                          message.amount,
                                                          message.amount,
                                                          message.termInMonths,
                                                          message.repayment)));

  boolean hasMessage(Class<?> clazz) {
    try {
      return getMessagesOfType(Messages.Default.class).size() > 0;
    } catch(Exception e) {
      return false;
    }
  }

  public void incomeShock() {
    if (hasMessage(Messages.Default.class)) {
      income = 1;
    } else {
      // 50% of households gain volatility income, the other 50% lose it.
      if (getPrng().discrete(1, 2).sample() == 1) {
        income += (getGlobals().incomeVolatility * income / 100);
      } else {
        income -= (getGlobals().incomeVolatility * income / 100);
      }
      if (income <= 0) {
        income = 1;
      }
    }
  }

  public void payTax() {
    if (income < getGlobals().topRateThreshold) {
      taxBill = (int) ((income - getGlobals().personalAllowance) * getGlobals().basicRate / 100);
    } else
      taxBill =
              (int)
                      (((income - getGlobals().topRateThreshold) * getGlobals().topRate / 100)
                              + (income - getGlobals().personalAllowance) * getGlobals().basicRate / 100);
    wealth -= taxBill / 12;
  }

  public void subsistenceConsumption() {
    creditScore = Math.min(100, creditScore + 1);
    wealth -= 5900 / 12;
    if (wealth < 0) {
      wealth = 1;
    }
  }

  public void discretionaryConsumption() {
    int incomeAfterSubsistence = income - 5900;
    double minLiqWealth =
            4.07 * Math.log(incomeAfterSubsistence) - 33.1 + getPrng().gaussian(0, 1).sample();
    double monthlyConsumption = 0.5 * Math.max(wealth - Math.exp(minLiqWealth), 0);
    wealth -= monthlyConsumption;
  }

  public void writeOff() {
    if (hasMessageOfType(Messages.Default.class)) {
      mortgage = null;
      creditScore = 0;
    }
  }

  public static class Mortgage {
    private int amount;
    private int balanceOutstanding;
    private int term;
    private int repayment;

    public Mortgage(int amount, int balanceOutstanding, int term, int repayment) {
      this.amount = amount;
      this.balanceOutstanding = balanceOutstanding;
      this.term = term;
      this.repayment = repayment;
    }
  }
}
