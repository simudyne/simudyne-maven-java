package org.example.models.advanced2;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Variable;

public class Household extends Agent<GlobalState> {
  @Variable int income;
  @Variable int wealth = 1000;

  Mortgage mortgage;
  int monthsInArrears = 0;
  int consumption = 3000;

  public void earnIncome() {
    wealth += income;
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
        broadcastMessage(new Messages.Arrears(monthsInArrears, mortgage.amount));

        // If we have spent more than 6 months in arrears,
        // default the mortgage (close it with 0 value).
        if (monthsInArrears > 6) {
          broadcastMessage(new Messages.CloseMortgage(0));
          mortgage = null;
        }
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
    return (mortgage == null) || (wealth >= mortgage.repayment);
  }

  public static Action<Household> applyForMortgage() {
    return Action.create(
        Household.class,
        h -> {
          if (h.mortgage == null) {
            int purchasePrice = h.income * 4;
            h.broadcastMessage(new Messages.MortgageApplication(purchasePrice, h.income, h.wealth));
          }
        });
  }

  public static Action<Household> takeOutMortgage() {
    return Action.create(
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
  }

  public void incomeShock() {
    income += 200 * getPrng().gaussian(0, 1).sample();

    if (income < 0) {
      income = 1;
    }
  }

  public void consume() {
    wealth -= consumption;

    if (wealth < 0) {
      wealth = 1;
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
