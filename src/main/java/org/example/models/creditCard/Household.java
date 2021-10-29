package org.example.models.creditCard;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.Random;

public class Household extends Agent<CreditCardModel.Globals> {

    @Variable(name = "Income")
    public int income;

    int wealth = 1000;
    int taxBill = 0;

    @Variable(name = "CrCC1 Balance")
    public double balance1;

    @Variable(name = "CrCC2 Balance")
    public double balance2;

    @Variable(name = "Credit Score")
    public double creditScore;

    @Variable(name = "Total Balance")
    public double totalBalance;

    @Variable(name = "Credit Limit")
    public double creditLimit;

    @Variable(name = "CrCC1 Balance Ratio")
    public double balanceSplitCC1 = 0.5;

    @Variable(name = "Learning Rate")
    public double learningRate = 0.05;

    Random random;

    void incomeShock() {
        // 50% of households gain volatility income, the other 50% lose it.
        if (getPrng().discrete(1, 2).sample() == 1) {
            income += (getGlobals().incomeVolatility * income / 100);
        } else {
            income -= (getGlobals().incomeVolatility * income / 100);
        }
        // Enforce Income cannot be negative
        if (income <= 0) {
            income = 1;
        }
    }

    void earnIncome() {
        wealth += income / 12.0;
    }

    void subsistenceConsumption() {
        wealth -= 5900 / 12.0;
    }

    void payCard() {
        if (wealth > totalBalance && creditScore > 0.8) {
            wealth -= totalBalance;
            totalBalance = 0;
            balance1 = 0;
            balance2 = 0;
            getLongAccumulator("convenienceUsers").add(1);
        }
        else {
            wealth -= totalBalance * getGlobals().minPayment;

            // TODO: change repayment based on credit score.
            balance1 -= balance1 * Math.max(getGlobals().minPayment,
                    (0.05 + random.nextDouble() * creditScore * getGlobals().avgPayment));
            balance2 -= balance2 * Math.max(getGlobals().minPayment,
                    (0.05 + random.nextDouble() * creditScore * getGlobals().avgPayment));
            totalBalance = balance1 + balance2;
            getLongAccumulator("revolvers").add(1);
        }
    }

    public static Action<Household> discretionaryConsumption =
            Action.create(Household.class,
                    household -> {

                        int incomeAfterSubsistence = household.income - 5900;
                        double minLiqWealth =
                                4.07 * Math.log(incomeAfterSubsistence) - 33.1 +
                                        household.getPrng().gaussian(0, 1).sample();
                        double monthlyConsumption = 0.5 * Math.max(household.wealth - Math.exp(minLiqWealth), 0);
                        household.wealth -= monthlyConsumption;

                        Messages.CreditCardOffer1 msg1 = household.getMessageOfType(Messages.CreditCardOffer1.class);
                        Messages.CreditCardOffer2 msg2 = household.getMessageOfType(Messages.CreditCardOffer2.class);
                        if(msg1 == null || msg2 == null) {
                            System.out.println("NO offer...");
                            return;
                        }

                        double rate1 = msg1.rate;
                        double rate2 = msg2.rate;

                        double rewardFactor1 = msg1.rewardFactor;
                        double rewardFactor2 = msg2.rewardFactor;

                        // Decide how to split the balance.
                        double required = monthlyConsumption * (household.getGlobals().creditCardUsage * (1.0 + household.random.nextGaussian() * 0.2));
                        required = Double.isNaN(required) ? 0.0 : required;

                        double f1 = Math.max(0.005, rate1 - rewardFactor1 * household.creditScore * household.creditScore);
                        double f2 = Math.max(0.005, rate2 - rewardFactor2 * household.creditScore * household.creditScore);

                        double target = f2 / (f1 + f2);
                        target += Math.signum(target - household.balanceSplitCC1) * household.random.nextDouble() * 0.01;

                        household.balanceSplitCC1 = Math.min(0.99,
                                Math.max(0.01, household.balanceSplitCC1 * (1.0 - household.learningRate) + target * household.learningRate));

                        double available1 = household.creditLimit - household.balance1;
                        double available2 = household.creditLimit - household.balance2;

                        double required1 = required * household.balanceSplitCC1;
                        double required2 = required * (1.0 - household.balanceSplitCC1);

                        double extra1 = Math.max(required1 - available1, 0.0);
                        double extra2 = Math.max(required2 - available2, 0.0);

                        household.balance1 += Math.min(required1, available1);
                        household.balance2 += Math.min(required2, available2);

                        available1 = household.creditLimit - household.balance1;
                        available2 = household.creditLimit - household.balance2;

                        if(extra1 > 0.0 && available2 > 0){
                            household.balance2 += Math.min(extra1, available1);
                        }

                        if(extra2 > 0.0 && available1 > 0){
                            household.balance1 += Math.min(extra2, available2);
                        }

                        household.totalBalance = household.balance1 + household.balance2;

                        //System.out.println("Rate1 = " + rate1 + "Rate2 =" + rate2 + "Reward=" + rewardFactor1 + "REward2=" + rewardFactor2);
                        //System.out.println("SENDING BALANCE ***---" + household.balance1 + "; " + household.balance2 + "----- SPLIT = " + household.balanceSplitCC1);

                        household.getLinks(Links.BalanceLink1.class).send(Messages.Balance1.class, (msg,link) -> {
                            msg.balance = household.balance1;
                            msg.score = household.creditScore;

                        });

                        household.getLinks(Links.BalanceLink2.class).send(Messages.Balance2.class, (msg, link) -> {
                            msg.balance = household.balance2;
                            msg.score = household.creditScore;
                        });
                    });
}

