package org.example.models.advanced1;

import simudyne.core.abm.Agent;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Variable;

public class Household extends Agent<GlobalState> {
  @Variable private int income = 5000;
  @Variable private int consumption = 3000;
  @Variable private int wealth = 1000;
  @Variable private int repayment = 100;

  void consume() {
    wealth -= consumption;
  }

  void earnIncome() {
    wealth += income;
  }

  void payMortgage() {
    if (canPay()) {
      getLinks(Links.BankLink.class).send(Messages.RepaymentAmount.class, repayment);
    }
  }

  private Boolean canPay() {
    return wealth >= repayment;
  }
}
