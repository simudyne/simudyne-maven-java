package org.example.models.conway;

import providence.simucore.abm.Agent;
import providence.simucore.abm.AgentEnvironment;
import providence.simucore.annotations.Variable;

public class Cell implements Agent {
  @Variable public boolean alive;

  public Cell(boolean alive) {
    this.alive = alive;
  }

  public void calculate(AgentEnvironment env) {
    env.hasMessageOfType(
        Messages.Start.class,
        message -> {
          env.broadcastMessage(new Messages.Neighbour(alive));
        });

    env.hasMessagesOfType(
        Messages.Neighbour.class,
        messages -> {
          long count = messages.stream().filter(m -> m.getBody().alive).count();

          if (alive && (count < 2 || count > 3)) {
            env.getLongAccumulator("died").add(1);
            alive = false;
          } else if (!alive && count == 3) {
            env.getLongAccumulator("born").add(1);
            alive = true;
          }
        });
  }
}
