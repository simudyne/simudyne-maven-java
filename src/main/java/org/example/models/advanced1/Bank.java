package org.example.models.advanced1;

import simudyne.core.abm.Agent;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Variable;

import java.util.List;

public class Bank extends Agent<GlobalState> {
  @Variable private int debt = 90;

  void updateBalanceSheet() {
    int assets = 0;
    List<Messages.RepaymentAmount> paymentMessages =
        getMessagesOfType(Messages.RepaymentAmount.class);
    for (Messages.RepaymentAmount payment : paymentMessages) {
      assets += payment.getBody();
    }

    getLongAccumulator("assets").add(assets);
    getLongAccumulator("equity").add(assets - this.debt);
  }
}
