package org.example.models.tokyo;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Variable;

public class SmallBank extends Agent<TokyoModel.Globals> {

    private static double z95 = 1.645; //z-score for 95% normal distribution

    @Variable
    double fundamentalTerm;

    double chartTerm;

    double noiseTerm;

    double fundamentalWeight;

    double chartWeight;

    double noiseWeight;

    double tao = 1.0;

    @Variable
    double capAdequacy;

    double netWorth;

    double valueAtRisk;

    double stockNum = 1500;

    boolean bankrupt = false;

    double expReturn;

    double expPrice;

    private int netLending;

    static int taoi = 3;

    public static Action<SmallBank> generateWeightsFromUniform() {
        return Action.create(SmallBank.class, smallBank -> {

            smallBank.fundamentalWeight = smallBank.getPrng().uniform(0.0, 1.0).sample();
            smallBank.chartWeight = smallBank.getPrng().uniform(0.0, 1.0).sample();
            smallBank.noiseWeight = smallBank.getPrng().uniform(0.0, 1.0).sample();

            double sum = smallBank.fundamentalWeight + smallBank.chartWeight + smallBank.noiseWeight;

            smallBank.fundamentalWeight = smallBank.fundamentalWeight * (1.0 / sum);
            smallBank.chartWeight = smallBank.chartWeight * (1.0 / sum);
            smallBank.noiseWeight = smallBank.noiseWeight * (1.0 / sum);
        });
    }

    public static Action<SmallBank> generateWeightsFromGaussian() {
        return Action.create(SmallBank.class, smallBank -> {

            smallBank.fundamentalWeight = smallBank.getPrng().gaussian(5.0, 1.0).sample();
            smallBank.chartWeight = smallBank.getPrng().gaussian(5.0, 1.0).sample();
            smallBank.noiseWeight = smallBank.getPrng().gaussian(5.0, 1.0).sample();

            double sum = smallBank.fundamentalWeight + smallBank.chartWeight + smallBank.noiseWeight;

            smallBank.fundamentalWeight = smallBank.fundamentalWeight * (1.0 / sum);
            smallBank.chartWeight = smallBank.chartWeight * (1.0 / sum);
            smallBank.noiseWeight = smallBank.noiseWeight * (1.0 / sum);


        });
    }


    public static Action<SmallBank> computeFundamentalTerm() {
        return Action.create(SmallBank.class, smallBank -> {
            smallBank.fundamentalTerm = (1 / smallBank.getGlobals().taoMeanSB) * Math.log(
                    smallBank.getGlobals().logicalPrice
                            / smallBank.getGlobals().riskAssetPrice); // eqn 7
        });
    }


    public static Action<SmallBank> computeChartTerm() {
        return Action.create(SmallBank.class, smallBank -> {
            // TODO: Complete this calculation
            smallBank.chartTerm =
                    1 / smallBank.tao * Math.log(smallBank.getGlobals().riskAssetPrice_1
                            / smallBank.getGlobals().riskAssetPrice_2);
            // TODO: Check as simplified version of eqn 8 for when j = 1
        });
    }

    public static Action<SmallBank> computeNoiseTerm() {
        return Action.create(SmallBank.class, smallBank -> {
            smallBank.noiseTerm =
                    smallBank.getPrng().gaussian(0.0, smallBank.getGlobals().sigmaE).sample();
        });
    }

    public static Action<SmallBank> computeVaR() {
        return Action.create(SmallBank.class, smallBank -> {
            smallBank.valueAtRisk =
                smallBank.getGlobals().sigma
                        * (smallBank.stockNum * smallBank.getGlobals().riskAssetPrice)
                        * z95;

            //System.out.println(smallBank.valueAtRisk);
        });
    }

    public static Action<SmallBank> buyOrSell() {
        return Action.create(SmallBank.class, smallBank -> {
            // Deal with Bankrupt Banks
            if (!smallBank.bankrupt) {
                smallBank.expReturn =
                        1 / (smallBank.fundamentalWeight + smallBank.chartWeight + smallBank.noiseWeight)
                                * (smallBank.fundamentalWeight * smallBank.fundamentalTerm
                                + smallBank.noiseWeight * smallBank.noiseTerm
                                + smallBank.chartWeight * smallBank.chartTerm); // eqn 5

                smallBank.expPrice =
                        smallBank.getGlobals().riskAssetPrice
                                * Math.exp(smallBank.expReturn * smallBank.tao);

                if (smallBank.expPrice >= smallBank.getGlobals().riskAssetPrice) {
                    // Buy a stock
                    smallBank.getGlobals().nbRiskAssetsBought += 1;
                    smallBank.stockNum += 1;
                } else {
                    // Sell a stock
                    smallBank.getGlobals().nbRiskAssetsSold += 1;
                    smallBank.stockNum -= 1;
                }
            }
        });
    }

    public static Action<SmallBank> updateCapitalAdequacy() {
        return Action.create(SmallBank.class, smallBank -> {
            if (!smallBank.bankrupt) {

                // Make the amount of interbank lending count towards the Net Worth of the bank.
                smallBank.netLending = smallBank.getLinks().size() * 20; // each loan is for 20

                // TODO: Generalise this concept so that each bank loses the value of loans to banks which go bankrupt
                smallBank.netWorth = (smallBank.stockNum * smallBank.getGlobals().riskAssetPrice)
                        + smallBank.netLending; // added as omitted
                smallBank.capAdequacy = smallBank.netWorth / (smallBank.valueAtRisk * smallBank.stockNum);// eqn 9

            }
        });
    }

    public static Action<SmallBank> checkCapitalAdequacy() {
        return Action.create(SmallBank.class, smallBank -> {
            if (!smallBank.bankrupt) {
                if (smallBank.capAdequacy < 0.04) {
                    smallBank.capAdequacy = 0.04;
                    smallBank.bankrupt = true; // This is incorrect in the paper - I have corrected it.
                    smallBank.getLongAccumulator("nbDefaults").add(1);
                    smallBank.removeLinks();
//                    smallBank.stop();
                }
            }
        });
    }


}
