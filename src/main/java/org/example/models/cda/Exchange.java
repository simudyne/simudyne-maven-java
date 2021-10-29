package org.example.models.cda;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;

public class Exchange extends Agent<CDAModel.Globals> {

    private double obid = 0.0;
    private int obidQ = 0;
    private Trader obidOwner = null;

    private double oask = 1000.0;
    private int oaskQ = 0;
    private Trader oaskOwner = null;

    private List<Double> returns = new ArrayList<Double>() {};

    private double transactionPrice = Double.NaN;

    public static Action<Exchange> clear() {
        return Action.create(Exchange.class, exchange -> {


            boolean bidUpdated = false;
            for(Messages.Bid bid : exchange.getMessagesOfType(Messages.Bid.class)) {

                if(bid.price > exchange.obid) {
                    exchange.obid = bid.price;
                    exchange.obidQ = bid.quantity;
                    exchange.obidOwner = bid.owner;
                    bidUpdated = true;
                }
            }

            if(bidUpdated){
                exchange.getLinks(Links.TraderLink.class).send(Messages.Bid.class, (b, link) -> {
                    b.price = exchange.obid;
                    b.quantity = exchange.obidQ;
                    b.owner = exchange.obidOwner;
                });
            }

            // Update outstanding ask.
            boolean askUpdated = false;
            for(Messages.Ask ask : exchange.getMessagesOfType(Messages.Ask.class)) {

                if(ask.price < exchange.oask){
                    exchange.oask = ask.price;
                    exchange.oaskQ = ask.quantity;
                    exchange.oaskOwner = ask.owner;
                    askUpdated = true;
                }
            }

            if(askUpdated){
                exchange.getLinks(Links.TraderLink.class).send(Messages.Ask.class, (a, link) -> {
                    a.price = exchange.oask;
                    a.quantity = exchange.oaskQ;
                    a.owner = exchange.oaskOwner;
                });
            }

            // Clear market.
            if(bidUpdated || askUpdated) {
                if(exchange.obid >= exchange.oask && exchange.obidQ > 0 && exchange.oaskQ > 0) {
                    int clearedQ = Math.min(exchange.obidQ, exchange.oaskQ);

                    exchange.obidQ = Math.max(exchange.obidQ - clearedQ, 0);
                    exchange.oaskQ = Math.max(exchange.oaskQ - clearedQ, 0);

                    double oldTransactionPrice = exchange.transactionPrice;
                    exchange.transactionPrice = 0.5f * exchange.obid + 0.5f * exchange.oask;

                    if(exchange.obidQ == 0)
                        exchange.obid = 0.0;

                    if(exchange.oaskQ == 0)
                        exchange.oask = 1000.0;

                    exchange.getLinks(Links.TraderLink.class).send(Messages.Transaction.class, (transaction,link) -> {
                        transaction.bid = exchange.obidQ;
                        transaction.ask = exchange.oaskQ;
                        transaction.quantity = clearedQ;
                        transaction.price = exchange.transactionPrice;
                    });

                    if(!Double.isNaN(oldTransactionPrice)) {
                        double relRet = exchange.transactionPrice / oldTransactionPrice - 1.0;

                        // Ignoring zero returns - no new transactions.
                        if(Math.abs(relRet) > 1e-8)
                            exchange.returns.add(relRet);

                        if(exchange.returns.size() > 1000)
                            exchange.returns.remove(0);
                    }
                }
            }
        });
    }

    public static Action<Exchange> update() {
        return Action.create(Exchange.class, exchange -> {

            if(!Double.isNaN(exchange.transactionPrice)) {
                exchange.getDoubleAccumulator("transactions").add(exchange.transactionPrice);
            }

            if(exchange.returns.size() > 200) {

                Kurtosis kurtosis = new Kurtosis();

                double[] target = new double[exchange.returns.size()];
                for (int i = 0; i < exchange.returns.size(); i++) {
                    target[i] = exchange.returns.get(i);
                }

                kurtosis.setData(target);
                //System.out.println("KURTOSIS: " + kurtosis.evaluate());
                exchange.getDoubleAccumulator("kurtosis").add(kurtosis.evaluate());
            }

            if(exchange.obid > 0.0 && exchange.oask < 1000.0) {
                exchange.getDoubleAccumulator("spread").add(exchange.oask - exchange.obid);
            }

            //System.out.println("Bids=" + exchange.obid + " Asks=" + exchange.oask);
        });
    }
}

