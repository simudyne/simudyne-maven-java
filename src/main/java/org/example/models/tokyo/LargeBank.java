package org.example.models.tokyo;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

public class LargeBank extends Agent<TokyoModel.Globals> {

    @Variable
    double fundamentalTerm;

    double chartTerm;

    double noiseTerm;

    double fundamentalWeight;

    double chartWeight;

    double noiseWeight;

    double tao = 1.0;

    double capAdequacy;

    double netWorth;

    double valueAtRisk = 0.01;

    double stockNum;

    boolean bankrupt = false;

    double expReturn;

    double expPrice;

    static int taoi = 3;

    public static Action<LargeBank> generateWeightsFromUniform() {
        return Action.create(LargeBank.class, largeBank -> {

            largeBank.fundamentalWeight = largeBank.getPrng().uniform(0.0, 1.0).sample();
            largeBank.chartWeight = largeBank.getPrng().uniform(0.0, 1.0).sample();
            largeBank.noiseWeight = largeBank.getPrng().uniform(0.0, 1.0).sample();

            double sum = largeBank.fundamentalWeight + largeBank.chartWeight + largeBank.noiseWeight;

            largeBank.fundamentalWeight = largeBank.fundamentalWeight * (1.0 / sum);
            largeBank.chartWeight = largeBank.chartWeight * (1.0 / sum);
            largeBank.noiseWeight = largeBank.noiseWeight * (1.0 / sum);
        });
    }

    public static Action<LargeBank> generateWeightsFromGaussian() {
        return Action.create(LargeBank.class, largeBank -> {

            largeBank.fundamentalWeight = largeBank.getPrng().gaussian(5.0, 1.0).sample();
            largeBank.chartWeight = largeBank.getPrng().gaussian(5.0, 1.0).sample();
            largeBank.noiseWeight = largeBank.getPrng().gaussian(5.0, 1.0).sample();

            double sum = largeBank.fundamentalWeight + largeBank.chartWeight + largeBank.noiseWeight;

            largeBank.fundamentalWeight = largeBank.fundamentalWeight * (1.0 / sum);
            largeBank.chartWeight = largeBank.chartWeight * (1.0 / sum);
            largeBank.noiseWeight = largeBank.noiseWeight * (1.0 / sum);


        });
    }


    public static Action<LargeBank> computeFundamentalTerm() {
        return Action.create(LargeBank.class, largeBank -> {
            largeBank.fundamentalTerm = (1 / largeBank.getGlobals().taoMeanLB) * Math.log(
                    largeBank.getGlobals().logicalPrice
                            / largeBank.getGlobals().riskAssetPrice); // eqn 7
        });
    }


    public static Action<LargeBank> computeChartTerm() {
        return Action.create(LargeBank.class, largeBank -> {
            // TODO: Complete this calculation
            largeBank.chartTerm =
                    1 / largeBank.tao * Math.log(largeBank.getGlobals().riskAssetPrice_1
                    / largeBank.getGlobals().riskAssetPrice_2);
            // TODO: Check as simplified version of eqn 8 for when j = 1
        });
    }

    public static Action<LargeBank> computeNoiseTerm() {
        return Action.create(LargeBank.class, largeBank -> {
            largeBank.noiseTerm =
                    largeBank.getPrng().gaussian(0.0, largeBank.getGlobals().sigmaE).sample();
        });
    }

    public static Action<LargeBank> checkCapitalAdequacy() {
        return Action.create(LargeBank.class, largeBank -> {
            if (!largeBank.bankrupt) {
                if (largeBank.capAdequacy < 0.04) {
                    largeBank.bankrupt = true; // This is incorrect in the paper - I have corrected it.
                    largeBank.getLongAccumulator("nbDefaults").add(1);
                }
            }
        });
    }

    public static Action<LargeBank> buyOrSell() {
        return Action.create(LargeBank.class, largeBank -> {
            // Deal with Bankrupt Banks
            if (!largeBank.bankrupt) {
                largeBank.expReturn =
                        1 / (largeBank.fundamentalWeight + largeBank.chartWeight + largeBank.noiseWeight)
                                * (largeBank.fundamentalWeight * largeBank.fundamentalTerm
                                + largeBank.noiseWeight * largeBank.noiseTerm
                                + largeBank.chartWeight * largeBank.chartTerm); // eqn 5

                largeBank.expPrice =
                        largeBank.getGlobals().riskAssetPrice
                                * Math.exp(largeBank.expReturn * largeBank.tao);

                if (largeBank.expPrice >= largeBank.getGlobals().riskAssetPrice) {
                    // Buy a stock
                    //TODO Complete this
                    largeBank.getGlobals().nbRiskAssetsBought += 1;
                    largeBank.stockNum += 1;
                } else {
                    // Sell a stock
                    //TODO Complete this
                    largeBank.getGlobals().nbRiskAssetsSold += 1;
                    largeBank.stockNum -= 1;
                }
            }
        });
    }

    public static Action<LargeBank> updateCapitalAdequacy() {
        return Action.create(LargeBank.class, largeBank -> {
            if (!largeBank.bankrupt) {
                largeBank.netWorth = largeBank.stockNum * largeBank.getGlobals().riskAssetPrice; // added as omitted
                largeBank.capAdequacy = largeBank.netWorth / largeBank.valueAtRisk * largeBank.stockNum;// eqn 9
                // TODO: Update the VaR numbers as there is no description in the paper.
            }
        });
    }
}

