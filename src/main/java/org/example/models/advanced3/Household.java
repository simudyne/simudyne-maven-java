package org.example.models.advanced3;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

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
        monthsInArrears += 1;
        broadcastMessage(new Messages.Arrears(monthsInArrears, mortgage.balanceOutstanding));
      }
    }
  }

  private void checkMaturity() {
    if (mortgage.term == 0) {
      broadcastMessage(new Messages.CloseMortgage(mortgage.amount));
      mortgage = null;
    } else {
      broadcastMessage(new Messages.Payment(mortgage.repayment, mortgage.amount));
    }
  }

  private Boolean canPay() {
    return wealth >= mortgage.repayment;
  }

  public static Action<Household> applyForMortgage =
      Action.create(
          Household.class,
          h -> {
            if (h.mortgage == null) {
              if (h.getPrng().discrete(1, 5).sample() == 1) {
                int purchasePrice = 100000 + h.income * 2;
                h.broadcastMessage(
                    new Messages.MortgageApplication(purchasePrice, h.income, h.wealth));
              }
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
                              message.getBody().amount,
                              message.getBody().amount,
                              message.getBody().termInMonths,
                              message.getBody().repayment)));

  public void incomeShock() {
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
    wealth -= 5900 / 12;

    if (wealth < 0) {
      wealth = 1;
    }
  }

  public void discretionaryConsmption() {
    int incomeAfterSubsistence = income - 5900;
    double minLiqWealth =
        4.07 * Math.log(incomeAfterSubsistence) - 33.1 + getPrng().gaussian(0, 1).sample();
    double monthlyConsumption = 0.5 * Math.max(wealth - Math.exp(minLiqWealth), 0);
    wealth -= monthlyConsumption;
  }

  public void writeOff() {
    if (getMessageOfType(Boolean.class).getBody()) {
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
