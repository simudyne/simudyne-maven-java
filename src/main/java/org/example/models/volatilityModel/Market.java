package org.example.models.volatilityModel;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.functions.SerializableConsumer;

import java.util.List;

public class Market extends Agent<VolatilityModel.Globals> {

    /**
     * The following arrays represent time series data for the relative constants with the value for the current
     * time step being held in the 0th component of the array.
     *
     * so Gf = Gf[t,t-1,t-2]
     */

    //performance time series
    double Gf[] = {0, 0, 0};
    double Gc[] = {0, 0, 0};

    //performance over time time series
    double Wf[] = {0, 0, 0};
    double Wc[] = {0, 0, 0};

    //demands time series
    double Df[] = {0,0,0};
    double Dc[]  = {0,0,0};

    //adaption time series
    double[] A = {0, 0};

    //fraction of each agent
    double Nf[] = {0.5,0.5};
    double Nc[] = {0.5,0.5};

    double Pcf = 0;
    double Pfc = 0;
    double v = 0.05;

    //price time series
    double price[] = {0, 0, 0};

    double mu = 0.01;
    double eta = 0.991;
    double beta = 1;

   //noise constants for demand
   double sigma_f=0.681;
   double sigma_c=1.724;

    //convenience method that returns an action of correct type.
    private static Action<Market> action(SerializableConsumer<Market> injector) {
        return Action.create(Market.class, injector);
    }

    //market sends the latest prices to the traders at the start of each step.
    static Action<Market> distributePriceInformation = action(market -> {
        market.getLinks(Links.MrktToTraderLink.class).send(Messages.PriceInfo.class, (mes, links) -> {
            mes.price = market.price;
        });
    });

    //calculates the performance of the two strategies.
    static Action<Market> calculatePortfolioPerformance = action(market -> {
        market.Gf[1] = market.Gf[0];
        market.Gf[0] = ( Math.exp(market.price[0]) - Math.exp(market.price[1]) ) * market.Df[1];
        market.getDoubleAccumulator("portfolio_performance_fundamentalists").add(market.Gf[0]);

        market.Gc[1] =  market.Gc[0];
        market.Gc[0] = ( Math.exp(market.price[0]) - Math.exp(market.price[1]) ) * market.Dc[1];
        market.getDoubleAccumulator("portfolio_performance_chartists").add(market.Gc[0]);

        double tempWf1 = market.Wf[0];
        market.Wf[0] = market.eta * market.Wf[0] + (1 - market.eta) * market.Gf[0];
        market.Wf[1] = tempWf1;

        double tempWc1 = market.Wc[0];
        market.Wc[0] = market.eta * market.Wc[0] + (1 - market.eta) * market.Gc[0];
        market.Wc[1] = tempWc1;
    });


    //calculates the fraction of each trader required using the DCA strategy.
    static Action<Market> calculateFractionOfEachStrategies = action(market -> {
        if(market.getGlobals().DCATransitionStrategy && market.getGlobals().TPATransitionStrategy) {
            throw new IllegalArgumentException("Only choose one strategy");
        }

        if(market.getGlobals().DCATransitionStrategy) {
            market.Nf[1] = market.Nf[0];
            market.Nf[0] = Math.min(1, market.DCATransitionStrategy());

            market.Nc[1]  = market.Nc[0];
            market.Nc[0] = 1-market.Nf[0];
        }

        else if (market.getGlobals().TPATransitionStrategy) {
            market.Nf[1] = market.Nf[0];
            market.Nf[0] = Math.min(1, market.TPATransitionStrategy());

            market.Nc[1] = market.Nc[0];
            market.Nc[0] = 1 - market.Nf[0];
        }
        market.getLinks(Links.MrktToTraderLink.class).send(Messages.RatioOfFundementals.class, market.Nf[0]);
        market.getDoubleAccumulator("ratioOfFundamentalists").add(market.Nf[0]);
        market.getDoubleAccumulator("ratioOfChartists").add(market.Nc[0]);
    });

    //calculates the fraction of each trader required using the DCA strategy.
    private double DCATransitionStrategy() {
        return Nf[0] = 1 / (1 + Math.exp(-beta * A[0]));
    }

    //calculates the fraction of each trader required using the TPA strategy.
    private double TPATransitionStrategy() {
        Pcf = Math.min(1, v* Math.exp(A[0]));
        Pfc = Math.min(1, v* Math.exp(-A[0]));
        return Nf[0] + Nc[1]*Pcf - Nf[1]*Pfc;
    }


    //calculates the adaption value used in governing agent switching
    static Action<Market> calculateAdaptionValue = action(market -> {
        double tempPrevA = market.A[0];
        market.A[0] = market.getGlobals().alpha_w * (market.Wf[0] - market.Wc[0])
                + market.getGlobals().alpha_o + market.getGlobals().alpha_p * Math.pow((0 - market.price[0]) , 2 );
        market.A[1]=tempPrevA;
    });

    //calculates demand and new price
    static Action<Market> calcTotalDemandAndNewPrice = action(market -> {

        //Receive the lists of bids this round.
        List<Messages.DemandMessage> demandMessages = market.getMessagesOfType(Messages.DemandMessage.class);

        //calculate the number of fundamentalists bidding this round.
        double numberF = demandMessages.stream()
                .filter(mes -> mes.behaviour.equals(Behaviour.FUNDAMENTALIST))
                .count();
        //avoid division by zero
        numberF  = numberF > 0 ? numberF : 1;
        //calculate the number of trend followers bidding this round.
        double numberC = demandMessages.stream()
                .filter(mes -> mes.behaviour.equals(Behaviour.CHARTISTS))
                .count();
        //avoid division by zero
        numberC  = numberC > 0 ? numberC : 1;


        //calculate fundamentalist demands and shift time series.
        double tempDf1 = market.Df[0];
        market.Df[0] = market.calcDemand(demandMessages, Behaviour.FUNDAMENTALIST)/(numberF)
                + (market.getPrng().normal(0,1).sample()* market.sigma_f );
        market.getDoubleAccumulator("fundamentalistDemand").add(market.Df[0]);
        market.Df[1] = tempDf1;

         //calculate trend follower demands and shift time series
        double tempDc1 = market.Dc[0];
        market.Dc[0] = market.calcDemand(demandMessages, Behaviour.CHARTISTS)/(numberC)
                + (market.getPrng().normal(0,1).sample()* market.sigma_c );
        market.getDoubleAccumulator("trendFollowerDemand").add(market.Dc[0]);
        market.Dc[1] = tempDc1;

        //calculate the new price
        market.calculateNewPrice();
    });

    //convenience method to calculate total demand
    private double calcDemand(List<Messages.DemandMessage> demandMessages, Behaviour behaviour) {
        return demandMessages.stream()
                .filter(mes -> mes.behaviour.equals(behaviour))
                .mapToDouble(mes -> mes.demand)
                .sum();
    }

    //convenience method to calculate new price
    private void calculateNewPrice() {
        double tempPrice2 = price[1];
        double tempPrice1 = price[0];
        getDoubleAccumulator("price").add(Math.exp(price[0]));
        getDoubleAccumulator("returns").add(price[0]-price[1]);
        price[0] = price[0] + mu * (Nf[0]*Df[0] + Nc[0]*Dc[0]);
        price[2] = tempPrice2;
        price[1] = tempPrice1;
    }
}
