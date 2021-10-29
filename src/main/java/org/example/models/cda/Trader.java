package org.example.models.cda;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

import java.util.List;
import java.util.Random;

/**
 * Zero-intelligence trader that submits a bid or an ask from a uniform distribution U(100., 200.).
 * Strategy does not require listening to exchange messages.
 */

public class Trader extends Agent<CDAModel.Globals> {

    static Random random = new Random();

    double tradingThreshold = 0.1 + random.nextFloat() * 0.4;

    private boolean isBuyer = random.nextBoolean();

    private double limitPrice = 100.0 + 100.0 * random.nextDouble();

    private double outstandingBid = 0.;
    private double outstandingAsk = 1000.;

    private boolean isOutstandingTrader = false;

    public static Action<Trader> updateBelief() {
        return Action.create(Trader.class, trader -> {
            Messages.Prediction predict = trader.getMessagesOfType(Messages.Prediction.class).get(0);
            trader.limitPrice = Math.max(1., predict.mean - predict.scale * random.nextGaussian());
        });
    }

    public static Action<Trader> update() {
        return Action.create(Trader.class, trader -> {

            List<Messages.Bid> bids = trader.getMessagesOfType(Messages.Bid.class);
            if (bids.size() == 1) {
                Messages.Bid bid = bids.get(0);
                trader.outstandingBid = bid.price;
                trader.isOutstandingTrader = bid.owner == trader;
            }
            else if (bids.size() > 1) {
                throw new IllegalArgumentException("Single message expected from exchange.");
            }

            List<Messages.Ask> asks = trader.getMessagesOfType(Messages.Ask.class);
            if(asks.size() == 1) {
                Messages.Ask ask = asks.get(0);
                trader.outstandingAsk = ask.price;
                trader.isOutstandingTrader = ask.owner == trader;
            }
            else if (asks.size() > 1) {
                throw new IllegalArgumentException("Single message expected from exchange.");
            }
        });
    }

    public static Action<Trader> submitOrder() {
        return Action.create(Trader.class, trader -> {

            if (random.nextFloat() < trader.tradingThreshold && !trader.isOutstandingTrader) {
                if (trader.isBuyer) {
                    trader.submitBidOrder();
                } else {
                    trader.submitSellOrder();
                }
            }
        });
    }

    private void submitBidOrder() {
        getLinks(Links.ExchangeLink.class).send(Messages.Bid.class, (bid, link) -> {
            bid.quantity = 10 + random.nextInt(90);
            bid.price = limitPrice * random.nextDouble();
            bid.owner = this;
        });
    }

    private void submitSellOrder() {
        getLinks(Links.ExchangeLink.class).send(Messages.Ask.class, (ask, link) -> {
            ask.quantity = 10 + random.nextInt(90);
            ask.price = limitPrice + (300.0 - limitPrice) * random.nextDouble();
            ask.owner = this;
        });
    }
}

