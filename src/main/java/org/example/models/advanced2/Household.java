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
        send(
                Messages.Arrears.class,
                m -> {
                  m.monthsInArrears = monthsInArrears;
                  m.outstandingBalance = mortgage.amount;
                })
            .along(Links.BankLink.class)
            .execute();

        // If we have spent more than 6 months in arrears,
        // default the mortgage (close it with 0 value).
        if (monthsInArrears > 6) {
          send(Messages.MortgageCloseAmount.class, 0).along(Links.BankLink.class).execute();
          mortgage = null;
        }
      }
    }
  }

  private void checkMaturity() {
    if (mortgage.term == 0) {
      send(Messages.MortgageCloseAmount.class, mortgage.amount)
          .along(Links.BankLink.class)
          .execute();
      mortgage = null;
    } else {
      send(
              Messages.Payment.class,
              m -> {
                m.repayment = mortgage.repayment;
                m.amount = mortgage.amount;
              })
          .along(Links.BankLink.class)
          .execute();
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
            h.send(
                    Messages.MortgageApplication.class,
                    m -> {
                      m.amount = purchasePrice;
                      m.income = h.income;
                      m.wealth = h.wealth;
                    })
                .along(Links.BankLink.class)
                .execute();
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
                            message.amount,
                            message.amount,
                            message.termInMonths,
                            message.repayment)));
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
