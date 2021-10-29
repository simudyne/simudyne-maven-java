package org.example.models.mortgage;

import org.apache.commons.math3.random.EmpiricalDistribution;
import simudyne.core.abm.Action;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;
import simudyne.core.annotations.Variable;
import simudyne.core.graph.LongAccumulator;

@ModelSettings(macroStep = 60)
public class MortgageModel extends AgentBasedModel<MortgageModel.Globals> {

    public static final class Globals extends GlobalState {

        @Input(name = "LTI Limit")
        public double LTILimit = 4.5;

        @Input(name = "LTV Limit")
        public double LTVLimit = 0.95;

        @Input(name = "Interest Rate (%)")
        public double interestRate = 5.0;

        public int topRateThreshold = 45;

        public int personalAllowance = 11;

        public double basicRate = 20.0;

        public double topRate = 40.0;

        @Input(name = "Income Volatility (%)")
        public double incomeVolatility = 2.5;

        @Variable(name = "Stage 1 Provisions")
        public double stage1Provisions = 0.0;

        @Variable(name = "Stage 2 Provisions")
        public double stage2Provisions = 0.0;

        public int counter;

    }

    {
        createLongAccumulator("equity", "Bank Equity");
        createLongAccumulator("badLoans", "Bad Loans");
        createLongAccumulator("writeOffs", "Write-offs");
        createLongAccumulator("impairments", "Impairments");
        createLongAccumulator("debt", "Debt");
        createLongAccumulator("income", "Income");
        createLongAccumulator("mortgages", "Mortgages");
        createLongAccumulator("assets", "Assets");

        registerAgentTypes(mortgage.Bank.class, Household.class);
        registerLinkTypes(Links.HHBankLink.class);
    }

    public EmpiricalDistribution incomeDist = new mortgage.Distribution().getIncomeDistribution();
    public int wealth = 50000;

   // @Constant(name = "Number of Households")
    public long nbHouseholds = 200;

    @Override
    public void setup() {
        Group<Household> householdGroup =
                generateGroup(
                        Household.class,
                        nbHouseholds,
                        house -> {
                            house.income = (int) incomeDist.sample(1)[0];
                            house.wealth = wealth;
                        });

        Group<mortgage.Bank> bankGroup = generateGroup(mortgage.Bank.class, 1);

        householdGroup.partitionConnected(bankGroup, Links.HHBankLink.class);

        super.setup();
    }

    @Override
    public void step() {
        super.step();

        run(Household.applyForMortgage,
                mortgage.Bank.processApplication,
                Household.takeOutMortgage
        );

        run(
                Action.create(
                        Household.class,
                        (Household h) -> {
                            h.incomeShock();
                            h.earnIncome();
                            h.payTax();
                            h.subsistenceConsumption();
                            h.payMortgage();
                            h.discretionaryConsumption();
                        }),
                Action.create(
                        mortgage.Bank.class,
                        (mortgage.Bank b) -> {
                            b.accumulateIncome();
                            b.processArrears();
                            b.clearPaidMortgages();
                            b.updateAccumulators();
                        }),
                Action.create(Household.class, h -> h.writeOff()));
    }
}

