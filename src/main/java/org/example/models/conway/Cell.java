package org.example.models.conway;

import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.graph.Message;

import java.util.List;

public class Cell extends Agent<GameOfLife.Globals> {
  @Variable public boolean alive;

  public void onStart() {
    broadcastMessage(alive);
  }

  public void onNeighbourMessages() {
    long count = 0;

    List<Message<Boolean>> messages = getMessagesOfType(Boolean.class);
    for (Message<Boolean> m : messages) {
      if (m.getBody()) {
        count += 1;
      }
    }

    if (alive && (count < 2 || count > 3)) {
      getLongAccumulator("died").add(1);
      alive = false;
    } else if (!alive && count == 3) {
      getLongAccumulator("born").add(1);
      alive = true;
    }
  }
}
