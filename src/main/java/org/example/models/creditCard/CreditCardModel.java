package org.example.models.creditCard;

import mortgage.Distribution;
import org.apache.commons.math3.random.EmpiricalDistribution;
import simudyne.core.abm.*;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;

import java.util.Random;

@ModelSettings(macroStep = 100, ticks=200)
public class CreditCardModel extends AgentBasedModel<CreditCardModel.Globals> {

    public static class Globals extends GlobalState {

        //@Input(name = "Income Volatility (%)")
        public double incomeVolatility = 2.5;

        @Input(name = "Aggressiveness, Phoenix Bank")
        public double ell1 = 0.02;

        @Input(name = "Aggressiveness, Villagebank")
        public double ell2 = 0.02;

        //@Input(name = "Aggressiveness, Household")
        public double ellH = 0.05;

        @Input(name = "Initial Interest Rate, Phoenix Bank")
        public double rateOne = 0.1;

        @Input(name = "Initial Interest Rate, Villagebank")
        public double rateTwo = 0.1;

        //@Input(name = "Number of consumers")
        public int nbConsumers = 200;

        //@Input(name = "Average Credit Card Usage")
        public double creditCardUsage = 0.1;

        //@Input(name = "Minimum Repayment")
        public double minPayment = 0.05;

        //@Input(name = "Average Repayment")
        public double avgPayment = 0.2;

        @Input(name = "Market Share Estimate Noise")
        public double noise = 0.05;

        @Input(name = "Reward Factor, Phoenix Bank")
        public double rewardWeight1 = 0.5;

        @Input(name = "Reward Factor, Villagebank")
        public double rewardWeight2 = 0.0;

        @Input(name = "Update Frequency, Phoenix Bank")
        public int freq1 = 3;

        @Input(name = "Update Frequency, Villagebank")
        public int freq2 = 3;

        @Input(name = "What if Phoenix Bank discontinues reward programme @ T = 100?")
        public boolean hasRewardShock = false;

        @Input(name = "Market Share Target, Phoenix Bank")
        public double msTarget1 = 0.65;

        @Input(name = "Market Strategy, Villagebank")
        public double msTarget2 = 0.5;
    }

    public EmpiricalDistribution incomeDist = new Distribution().getIncomeDistribution();

    //private Random random = new Random();

    {

        createLongAccumulator("revolvers", "#Revolvers");
        createLongAccumulator("convenienceUsers", "#Convenience Users");

        createDoubleAccumulator("ms", "Market Share, Phoenix Bank");
        createDoubleAccumulator("rate1", "Interest Rate, Phoenix Bank");
        createDoubleAccumulator("rate2", "Interest Rate, Villagebank");
        createDoubleAccumulator("tbalance1", "Aggregate Balance, Phoenix Bank");
        createDoubleAccumulator("tbalance2", "Aggregate Balance, Villagebank");
        createDoubleAccumulator("rwe1", "Risk-weighted Exposure, Phoenix Bank");
        createDoubleAccumulator("rwe2", "Risk-weighted Exposure, Villagebank");

        registerAgentTypes(CreditCardProvider.class, Household.class);
        registerLinkTypes(Links.BalanceLink1.class, Links.BalanceLink2.class, Links.HHCreditCardLink1.class, Links.HHCreditCardLink2.class);
    }

    @Override
    public void setup() {

        Group<Household> householdGroup = generateGroup(Household.class, getGlobals().nbConsumers, household -> {
            household.income = (int)incomeDist.sample(1)[0];
            household.creditScore = 0.05 + 0.95 * household.getPrng().generator.nextDouble(); //random.nextBoolean() ? 0.2 : 1.0;
            household.creditLimit = Math.round(1000 * household.creditScore) * 10;
            household.learningRate = 0.1 * household.getPrng().generator.nextDouble() + (getGlobals().ellH - 0.05);
            household.random = new Random(household.getPrng().generator.nextLong());
        });

        Group<CreditCardProvider> creditCardProvider1 = generateGroup(CreditCardProvider.class, 1, ccp -> {
            ccp.rate = getGlobals().rateOne;
            ccp.rateAdaptive = ccp.rate;
            ccp.learningRate = Math.abs(getGlobals().ell1) < 1e-8 ? 0.0 : Math.max(0.0, 0.02 * ccp.getPrng().generator.nextDouble() + (getGlobals().ell1 - 0.01));
            ccp.ID = "Credit Card Provider 1";
            ccp.random = new Random(ccp.getPrng().generator.nextLong());
        });

        Group<CreditCardProvider> creditCardProvider2 = generateGroup(CreditCardProvider.class, 1, ccp -> {
            ccp.rate = getGlobals().rateTwo;
            ccp.rateAdaptive = ccp.rate;
            ccp.learningRate = Math.abs(getGlobals().ell2) < 1e-8 ? 0.0 : Math.max(0.0, 0.02 * ccp.getPrng().generator.nextDouble() + (getGlobals().ell2 - 0.01));
            ccp.ID = "Credit Card Provider 2";
            ccp.random = new Random(ccp.getPrng().generator.nextLong());
        });

        householdGroup.partitionConnected(creditCardProvider1, Links.BalanceLink1.class);
        householdGroup.partitionConnected(creditCardProvider2, Links.BalanceLink1.class);
        householdGroup.partitionConnected(creditCardProvider1, Links.BalanceLink2.class);
        householdGroup.partitionConnected(creditCardProvider2, Links.BalanceLink2.class);

        creditCardProvider1.partitionConnected(householdGroup, Links.HHCreditCardLink1.class);
        creditCardProvider2.partitionConnected(householdGroup, Links.HHCreditCardLink2.class);

        super.setup();

        step();
    }

    @Override
    public void step() {
        super.step();

        run(Action.create(Household.class, household -> {
            household.incomeShock();
            household.earnIncome();
            household.subsistenceConsumption();
            household.payCard();
        }));

        run(CreditCardProvider.sendCreditCardOffer,
                Household.discretionaryConsumption,
                CreditCardProvider.updateCreditCardOffer);
    }
}



