package org.example.models.advanced1;

import simudyne.core.abm.Agent;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Variable;
import simudyne.core.graph.Message;

import java.util.List;

public class Bank extends Agent<GlobalState> {
  @Variable int debt = 90;

  public void updateBalanceSheet() {
    int assets = 0;
    List<Message<Integer>> paymentMessages = getMessagesOfType(Integer.class);
    for (Message<Integer> payment : paymentMessages) {
      assets += payment.getBody();
    }

    getLongAccumulator("assets").add(assets);
    getLongAccumulator("equity").add(assets - this.debt);
  }
}
