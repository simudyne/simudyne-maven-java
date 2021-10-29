package org.example.models.creditCard;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CreditCardProvider extends Agent<CreditCardModel.Globals> {

    @Variable(name = "Rate")
    public double rate;

    public double rateAdaptive;

    public double learningRate;

    public String ID;

    Random random;

    @Variable(name = "Market Share (estimated)")
    public double marketShare = Double.NaN;

    private List<Double> mss = new ArrayList<>();

    public static Action<CreditCardProvider> updateCreditCardOffer =
            Action.create(CreditCardProvider.class, ccProvider -> {

                try{
                    if(ccProvider.ID == "Credit Card Provider 1") {

                        double wb1 = 0.0;
                        for (Messages.Balance1 b : ccProvider.getMessagesOfType(Messages.Balance1.class)) {
                            wb1 += b.balance;
                        }

                        double wb2 = 0.0;
                        for (Messages.Balance2 b : ccProvider.getMessagesOfType(Messages.Balance2.class)) {
                            wb2 += b.balance;
                        }

                        double target = wb1 / (wb1 + wb2);
                        double ms = Math.min(0.99, Math.max(0.01, ccProvider.random.nextGaussian() * ccProvider.getGlobals().noise + target));
                        ccProvider.mss.add(target);

                        // TODO: get average target.
                        if(!Double.isNaN(ccProvider.marketShare) && Math.abs(ms) > 1e-8) {
                            if(ms < ccProvider.getGlobals().msTarget1) {
                                ccProvider.rateAdaptive = ccProvider.rateAdaptive * (1.0 - ccProvider.random.nextDouble() * ccProvider.learningRate * Math.abs(ms / ccProvider.marketShare));
                            }
                            else {
                                //System.out.println("EXPLOIT---------------------------" + ccProvider.rateAdaptive + " --- and " + (1.0 + (0.2 + 0.8 * ccProvider.random.nextDouble()) * ccProvider.learningRate));
                                ccProvider.rateAdaptive = ccProvider.rateAdaptive * (1.0 + (0.2 + 0.8 * ccProvider.random.nextDouble()) * ccProvider.learningRate);
                            }
                        }

                        ccProvider.marketShare = Double.isNaN(ms) ? 0.5 : ms;

                        if(ccProvider.mss.size() == ccProvider.getGlobals().freq1) {
                            ccProvider.rate = Math.max(0.02, Math.min(0.4, ccProvider.rateAdaptive));
                            ccProvider.mss.clear();
                        }

                        ccProvider.getDoubleAccumulator("tbalance1").add(Double.isNaN(wb1) ? 0.0 : wb1);

                        wb1 = 0.0;
                        for (Messages.Balance1 b : ccProvider.getMessagesOfType(Messages.Balance1.class)) {
                            wb1 += b.balance * (0.0001 / b.score);
                        }

                        ccProvider.getDoubleAccumulator("rwe1").add(wb1);

                        ccProvider.getDoubleAccumulator("ms").add(ccProvider.marketShare);
                        ccProvider.getDoubleAccumulator("rate1").add(ccProvider.rate);

                        //System.out.println("Updating rate --" + ccProvider.ID + "---" + ccProvider.rate + "---" + ccProvider.learningRate);
                    }

                    if(ccProvider.ID == "Credit Card Provider 2") {

                        double wb1 = 0.0;
                        for (Messages.Balance1 b : ccProvider.getMessagesOfType(Messages.Balance1.class)) {
                            wb1 += b.balance;
                        }

                        double wb2 = 0.0;
                        for (Messages.Balance2 b : ccProvider.getMessagesOfType(Messages.Balance2.class)) {
                            wb2 += b.balance;
                        }

                        double target = wb2 / (wb1 + wb2);
                        double ms = Math.min(0.99, Math.max(0.01, ccProvider.random.nextGaussian() * ccProvider.getGlobals().noise + target));
                        ccProvider.mss.add(ccProvider.marketShare);

                        // TODO: get average target.
                        if(!Double.isNaN(ccProvider.marketShare) && Math.abs(ms) > 1e-8) {
                            if(ms < ccProvider.getGlobals().msTarget2) {
                                ccProvider.rateAdaptive = ccProvider.rateAdaptive * (1.0 - ccProvider.random.nextDouble() * ccProvider.learningRate * Math.abs(ms / ccProvider.marketShare));
                            }
                            else {
                                ccProvider.rateAdaptive = ccProvider.rateAdaptive * (1.0 + (0.2 + 0.8 * ccProvider.random.nextDouble()) * ccProvider.learningRate);
                                //ccProvider.rateAdaptive = ccProvider.rateAdaptive * (1.0 + ccProvider.random.nextDouble() * ccProvider.learningRate * Math.abs(ms / ccProvider.marketShare));
                            }
                        }

                        ccProvider.marketShare = Double.isNaN(ms) ? 0.5 : ms;

                        if(ccProvider.mss.size() == ccProvider.getGlobals().freq2) {
                            ccProvider.rate = Math.max(0.02, Math.min(0.4, ccProvider.rateAdaptive));
                            ccProvider.mss.clear();
                        }

                        //System.out.println("MARKET SHARE = " + ccProvider.marketShare);
                        ccProvider.getDoubleAccumulator("tbalance2").add(Double.isNaN(wb2) ? 0.0 : wb2);

                        wb2 = 0.0;
                        for (Messages.Balance2 b : ccProvider.getMessagesOfType(Messages.Balance2.class)) {
                            wb2 += b.balance * (0.0001 / b.score);
                        }

                        ccProvider.getDoubleAccumulator("rwe2").add(wb2);
                        ccProvider.getDoubleAccumulator("rate2").add(ccProvider.rate);

                        //System.out.println("Updating rate --" + ccProvider.ID + "---" + ccProvider.rate);
                    }
                }catch(NullPointerException e) {
                    //System.out.println("NO MESSAGES FROM CONSUMERS-----");
                }
            });

    private int counter = 0;

    public static Action<CreditCardProvider> sendCreditCardOffer =
            Action.create(CreditCardProvider.class,
                    ccProvider -> {

                        ccProvider.counter++;

                        //System.out.println("--" + ccProvider.ID);
                        if(ccProvider.ID == "Credit Card Provider 1") {
                            ccProvider.getLinks(Links.HHCreditCardLink1.class).send(Messages.CreditCardOffer1.class, (cc, link) -> {
                                //System.out.println("Sending offer " + ccProvider.ID + "----" + ccProvider.rate);
                                cc.rate = ccProvider.rate;
                                cc.rewardFactor = ccProvider.getGlobals().rewardWeight1;

                                if(ccProvider.getGlobals().hasRewardShock && ccProvider.counter >= 100) {
                                    cc.rewardFactor = 0.0;
                                }
                                else{
                                    cc.rewardFactor = ccProvider.getGlobals().rewardWeight1;
                                }

                                //System.out.println("Sending offer " + ccProvider.ID + "---" + ccProvider.counter +
                                //        " ::: " + cc.rewardFactor + "----" + ccProvider.rate);


                            });
                        }

                        if(ccProvider.ID == "Credit Card Provider 2") {
                            ccProvider.getLinks(Links.HHCreditCardLink2.class).send(Messages.CreditCardOffer2.class, (cc, link) -> {
                                cc.rate = ccProvider.rate;
                                cc.rewardFactor = ccProvider.getGlobals().rewardWeight2;
                            });
                        }
                    });
}


