package org.example.models.advanced1;

import simudyne.core.abm.Agent;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Variable;

public class Household extends Agent<GlobalState> {
  @Variable int income = 5000;
  @Variable int consumption = 3000;
  @Variable int wealth = 1000;
  @Variable int repayment = 100;

  public void consume() {
    wealth -= consumption;
  }

  public void earnIncome() {
    wealth += income;
  }

  public void payMortgage() {
    if (canPay()) {
      broadcastMessage(repayment);
    }
  }

  private Boolean canPay() {
    return wealth >= repayment;
  }
}
