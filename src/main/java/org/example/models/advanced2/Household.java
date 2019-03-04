package org.example.models.advanced2;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;
import simudyne.core.graph.Message;

public class Household extends Agent<GlobalState> {
  @Variable int income;
  @Variable int wealth = 1000;

  private Mortgage mortgage;
  private int monthsInArrears = 0;

  private static Action<Household> action(SerializableConsumer<Household> consumer) {
    return Action.create(Household.class, consumer);
  }

  void earnIncome() {
    wealth += income;
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
                ((arrears, bankLink) -> {
                  arrears.monthsInArrears = monthsInArrears;
                  arrears.outstandingBalance = monthsInArrears;
                }));
        // If we have spent more than 6 months in arrears,
        // default the mortgage (close it with 0 value).
        if (monthsInArrears > 6) {
          getLinks(Links.BankLink.class).send(Messages.MortgageCloseAmount.class, 0);
          mortgage = null;
        }
      }
    }
  }

  private void checkMaturity() {
    if (mortgage.term == 0) {
      getLinks(Links.BankLink.class).send(Messages.MortgageCloseAmount.class, mortgage.amount);
      mortgage = null;
    } else {
      getLinks(Links.BankLink.class)
          .send(
              Messages.Payment.class,
              ((payment, bankLink) -> {
                payment.repayment = mortgage.repayment;
                payment.amount = mortgage.amount;
              }));
    }
  }

  private Boolean canPay() {
    return (mortgage == null) || (wealth >= mortgage.repayment);
  }

  static Action<Household> applyForMortgage() {
    return action(
        h -> {
          if (h.mortgage == null) {
            int purchasePrice = h.income * 4;
            h.getLinks(Links.BankLink.class)
                .send(
                    Messages.MortgageApplication.class,
                    ((mortgageApplication, bankLink) -> {
                      mortgageApplication.amount = purchasePrice;
                      mortgageApplication.income = h.income;
                      mortgageApplication.wealth = h.wealth;
                    }));
          }
        });
  }

  static Action<Household> takeOutMortgage() {
    return action(
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

  void incomeShock() {
    income += 200 * getPrng().gaussian(0, 1).sample();

    if (income <= 0) {
      income = 1;
    }
  }

  void consume() {
    int consumption = 3000;
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

    Mortgage(int amount, int balanceOutstanding, int term, int repayment) {
      this.amount = amount;
      this.balanceOutstanding = balanceOutstanding;
      this.term = term;
      this.repayment = repayment;
    }
  }
}
