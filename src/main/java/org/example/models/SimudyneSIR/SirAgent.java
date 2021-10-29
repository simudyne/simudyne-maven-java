package org.example.models.SimudyneSIR;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

public class SirAgent extends Agent<SimudyneSIR.Globals> {
    public enum Status {SUSCEPTIBLE, INFECTED, RECOVERED}

    ;
    public Status status = Status.SUSCEPTIBLE;
    public int timeSinceInfected = 0;

    public static Action<SirAgent> step =
            Action.create(SirAgent.class, a -> {

                if (a.status == Status.INFECTED) {
                    // Loop through neighbors and stochastically expose them
                    a.getLinks(Links.SirLink.class).forEach(l -> {
                        double val = a.getPrng().uniform(0, 1).sample();
                        if (val < a.getGlobals().infectionProbability) {
                            a.send(Messages.InfectMessage.class).to(l.getTo());
                        }
                    });

                    if (a.timeSinceInfected++ > a.getGlobals().infectionDuration) {
                        a.status = Status.RECOVERED;
                    }
                }

                switch (a.status) {
                    case SUSCEPTIBLE:
                        a.getLongAccumulator("Susceptible").add(1);
                        break;
                    case INFECTED:
                        a.getLongAccumulator("Infected").add(1);
                        break;
                    case RECOVERED:
                        a.getLongAccumulator("Recovered").add(1);
                        break;
                }
            });

    public static Action<SirAgent> exposed =
            Action.create(SirAgent.class, a -> {
                if (a.hasMessageOfType(Messages.InfectMessage.class)) {
                    if (a.status == Status.SUSCEPTIBLE) {
                        a.status = Status.INFECTED;
                    }
                }
            });
}
