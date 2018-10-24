package org.example.models.advanced3;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

public class Household extends Agent<MortgageModel.Globals> {
  @Variable int income;
  @Variable int wealth = 1000;

  @Variable
  int repayment() {
    if (mortgage == null) {
      return 0;
    }

    return mortgage.repayment;
  }

  int taxBill = 0;
  Mortgage mortgage;
  int monthsInArrears = 0;

  private static Action<Household> action(SerializableConsumer<Household> consumer) {
    return Action.create(Household.class, consumer);
  }

  void earnIncome() {
    wealth += income / 12;
  }

  void payMortgage() {
    if (mortgage != null) {
      if (canPay()) {
        wealth -= mortgage.repayment;
        monthsInArrears = 0;
        mortgage.term -= 1;
        mortgage.balanceOutstanding -= mortgage.repayment;
        checkMaturity();
      } else {
        monthsInArrears += 1;
        getLinks(Links.BankLink.class)
            .send(
                Messages.Arrears.class,
                (m, l) -> {
                  m.monthsInArrears = monthsInArrears;
                  m.outstandingBalance = mortgage.balanceOutstanding;
                });
      }
    }
  }

  private void checkMaturity() {
    if (mortgage.term == 0) {
      getLinks(Links.BankLink.class).send(Messages.CloseMortgageAmount.class, mortgage.amount);
      mortgage = null;
    } else {
      getLinks(Links.BankLink.class)
          .send(
              Messages.Payment.class,
              (m, l) -> {
                m.repayment = mortgage.repayment;
                m.amount = mortgage.amount;
              });
    }
  }

  private Boolean canPay() {
    return wealth >= mortgage.repayment;
  }

  static Action<Household> applyForMortgage =
      action(
          h -> {
            if (h.mortgage == null) {
              if (h.getPrng().discrete(1, 5).sample() == 1) {
                int purchasePrice = 100000 + h.income * 2;
                h.getLinks(Links.BankLink.class)
                    .send(
                        Messages.MortgageApplication.class,
                        (m, l) -> {
                          m.amount = purchasePrice;
                          m.income = h.income;
                          m.wealth = h.wealth;
                        });
              }
            }
          });

  static Action<Household> takeOutMortgage =
      action(
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

  void incomeShock() {
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

  void payTax() {
    if (income < getGlobals().topRateThreshold) {
      taxBill = (int) ((income - getGlobals().personalAllowance) * getGlobals().basicRate / 100);
    } else {
      taxBill =
          (int)
              (((income - getGlobals().topRateThreshold) * getGlobals().topRate / 100)
                  + (income - getGlobals().personalAllowance) * getGlobals().basicRate / 100);
    }
    wealth -= taxBill / 12;
  }

  void subsistenceConsumption() {
    wealth -= 5900 / 12;

    if (wealth < 0) {
      wealth = 1;
    }
  }

  void discretionaryConsumption() {
    int incomeAfterSubsistence = income - 5900;
    double minLiqWealth =
        4.07 * Math.log(incomeAfterSubsistence) - 33.1 + getPrng().gaussian(0, 1).sample();
    double monthlyConsumption = 0.5 * Math.max(wealth - Math.exp(minLiqWealth), 0);
    wealth -= monthlyConsumption;
  }

  void writeOff() {
    if (hasMessageOfType(Messages.LoanDefault.class)) {
      mortgage = null;
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
