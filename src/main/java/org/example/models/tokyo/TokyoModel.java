package org.example.models.tokyo;

import org.apache.avro.generic.GenericData;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.stats.AgentStatisticsResult;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Variable;
import simudyne.core.data.CSVSource;

import java.util.ArrayList;
import java.util.List;


public class TokyoModel extends AgentBasedModel<TokyoModel.Globals> {

    //@Constant
    int nbLargeBanks = 10;

    //@Constant
    int nbSmallBanks = 90;

    //@Constant
    int networkType = 2; // 0: Core-periphery, 1: Scale-Free, 2: Random

    //@Constant
    int weightType = 0; // 0: Uniform, 1: Gaussian

    public static class Globals extends GlobalState {

        @Variable
        double riskAssetPrice = 10.0;

        double riskAssetPrice_1;

        double riskAssetPrice_2;

        @Variable
        int nbRiskAssetsBought = 0; // number of risk assets bought in previous period

        @Variable
        int nbRiskAssetsSold = 0; // risk assets sold in previous period

        @Constant
        double alpha = 0.1; // coefficient of price fluctuations

        @Constant
        int totRiskAssets = 100; // Total risk assets

        @Variable
        double logicalPrice = 10.0;

        double logicalPrice_1;

        @Constant
        double mu = 0.0;

        @Constant
        double sigma = 0.01;

        @Variable
        public double taoMeanLB;

        @Variable
        public double taoMeanSB;

        @Constant
        public double sigmaE = 1.0;
    }

    @Override
    public void init() {
        registerAgentTypes(LargeBank.class, SmallBank.class);
        registerLinkTypes(Links.selfLink.class, Links.largeToSmall.class);
        registerMessageTypes();
        createLongAccumulator("nbDefaults");
    }

    @Override
    public void setup() {

        // Core-periphery Network
        if (networkType == 0) {

            CSVSource linkSource = new CSVSource("InnerConnections.csv");
            List<Group> smallBankGroups = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                smallBankGroups.add(generateGroup(SmallBank.class, nbSmallBanks / 10));
            }

            Group largeBankGroup = generateGroup(LargeBank.class, nbLargeBanks);
            largeBankGroup.fullyConnected(largeBankGroup, Links.selfLink.class);

            for (int i = 0; i < 10; i++) {
                largeBankGroup.partitionConnected(smallBankGroups.get(i), Links.largeToSmall.class);
                smallBankGroups.get(i).loadConnections(smallBankGroups.get(i), Links.selfLink.class, linkSource);
            }
        }

        // Scale-free Network
        else if (networkType == 1) {

            CSVSource linkSource1 = new CSVSource("ScaleFree.csv");
            CSVSource linkSource2 = new CSVSource("ScaleFree.csv");
            CSVSource linkSource3 = new CSVSource("ScaleFree.csv");
            CSVSource linkSource4 = new CSVSource("ScaleFree.csv");

            Group largeBankGroupSF = generateGroup(LargeBank.class, 10);
            Group smallBankGroupSF = generateGroup(SmallBank.class, 90);

            largeBankGroupSF.loadConnections(largeBankGroupSF, Links.selfLink.class, linkSource1);
            largeBankGroupSF.loadConnections(smallBankGroupSF, Links.largeToSmall.class, linkSource2);
            smallBankGroupSF.loadConnections(smallBankGroupSF, Links.selfLink.class, linkSource3);
            smallBankGroupSF.loadConnections(largeBankGroupSF, Links.smallToLarge.class, linkSource4);
        }

        // Random Network (Erdős-Rényi graph)
        else if (networkType == 2) {

            CSVSource linkSource = new CSVSource("data/Random.csv");
            Group smallBankGroupRand = generateGroup(SmallBank.class, 100);
            smallBankGroupRand.loadConnections(smallBankGroupRand, Links.selfLink.class, linkSource);
        } else {
            throw new RuntimeException("Network Type should take a value of 0, 1 or 2. Try again. ");
        }
        super.setup();
        run(SmallBank.computeVaR());
        run(SmallBank.updateCapitalAdequacy());
    }


    @Override
    public void step() {
        super.step();

        // Some initialisation at the first tick
        if (getContext().getTick() == 0) {
            if (weightType == 0) {
                run(SmallBank.generateWeightsFromUniform());
            }
            else {
                run(SmallBank.generateWeightsFromGaussian());
            }
        } else {

            updatePrices();
            run(SmallBank.computeFundamentalTerm());
            run(SmallBank.computeChartTerm());
            run(SmallBank.computeNoiseTerm());
            run(SmallBank.computeVaR());
            run(SmallBank.buyOrSell());
            run(SmallBank.updateCapitalAdequacy());
            run(SmallBank.checkCapitalAdequacy());
            setLaggedPrices();

        }
    }

    public void updatePrices() {
        AgentStatisticsResult<LargeBank> taoStatsLB =
                select(LargeBank.class).stats().field("tao", b -> b.tao).get();
        getGlobals().taoMeanLB = taoStatsLB.getField("tao").getMean();

        AgentStatisticsResult<SmallBank> taoStatsSB =
                select(SmallBank.class).stats().field("tao", b -> b.tao).get();
        getGlobals().taoMeanSB = taoStatsSB.getField("tao").getMean();

        // Update risk asset price
        getGlobals().riskAssetPrice =
                getGlobals().riskAssetPrice
                        + getGlobals().alpha * getGlobals().riskAssetPrice
                        * (getGlobals().nbRiskAssetsBought - getGlobals().nbRiskAssetsSold)
                        / getGlobals().totRiskAssets; // eqn 2

        // Update logical price
        getGlobals().logicalPrice =
                getGlobals().logicalPrice
                        + (getGlobals().mu * getGlobals().logicalPrice)
                        + (getGlobals().sigma * getGlobals().logicalPrice
                        * getContext().getPrng().gaussian(0.0, 1.0).sample()); // Wiener process (GBM) //TODO check this

        getGlobals().nbRiskAssetsBought = 0;
        getGlobals().nbRiskAssetsSold = 0;

    }

    public void setLaggedPrices() {
        getGlobals().riskAssetPrice_1 = getGlobals().riskAssetPrice;
        getGlobals().riskAssetPrice_2 = getGlobals().riskAssetPrice_1;
        getGlobals().logicalPrice_1 = getGlobals().logicalPrice;

    }
}