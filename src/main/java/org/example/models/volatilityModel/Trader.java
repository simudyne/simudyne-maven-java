package org.example.models.volatilityModel;


import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.functions.SerializableConsumer;

public class Trader extends Agent<VolatilityModel.Globals> {

    //Enum representing the behaviour of each agent
    public Behaviour behaviour;

    //price time series
    double price[] = {0,0,0};
    double phi = 1.00;
    double chi = 1.20;
    double fundamental = 0.00;

    //convenience method that returns an action of correct type.
    private static Action<Trader> action(SerializableConsumer<Trader> injector) {
        return Action.create(Trader.class, injector);
    }

    //receives the latest price from the market.
    static Action<Trader> receivePriceInformation = action(trader -> {
        trader.price = trader.getMessageOfType(Messages.PriceInfo.class).price;
    });

    //adapts trader strategies dependant on the discrete amount 'required'
    static Action<Trader> changeStrategies = action(trader -> {
        double pf = trader.getMessageOfType(Messages.RatioOfFundementals.class).getBody();
        trader.behaviour = trader.getPrng().uniform(0,1)
                .sample() < pf ? Behaviour.FUNDAMENTALIST : Behaviour.CHARTISTS;
    });

    //sends the demand required dependant on the current market conditions
    static Action<Trader> sendDemand = action(trader -> {
        double demand = trader.behaviour.demand(trader.price);
        trader.getLinks(Links.TraderToMrktLink.class)
                .send(Messages.DemandMessage.class, (demandMessage, traderToMrktLink) -> {
            demandMessage.demand = demand;
            demandMessage.behaviour = trader.behaviour;
        });
    });
}
