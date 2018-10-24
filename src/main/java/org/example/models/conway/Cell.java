package org.example.models.conway;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.List;

public class Cell extends Agent<GameOfLife.Globals> {
  public static Action<Cell> action(SerializableConsumer<Cell> action) {
    return Action.create(Cell.class, action);
  }

  @Variable public boolean alive;

  public void onStart() {
    getLinks(Links.Neighbour.class).send(Messages.Alive.class, alive);
  }

  public void onNeighbourMessages() {
    long count = 0;

    List<Messages.Alive> messages = getMessagesOfType(Messages.Alive.class);
    for (Messages.Alive m : messages) {
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
