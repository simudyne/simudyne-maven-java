package org.example.models.advanced2;

import simudyne.core.abm.Action;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;
import simudyne.core.annotations.Variable;
import simudyne.core.graph.LongAccumulator;

@ModelSettings(macroStep = 120)
public class MortgageModel extends AgentBasedModel<GlobalState> {

  @Variable(name = "Bank Equity (£)")
  LongAccumulator accEquity = createLongAccumulator("equity");

  @Variable(name = "Bad Loans")
  LongAccumulator accBadLoans = createLongAccumulator("badLoans");

  @Variable(name = "Write-offs")
  LongAccumulator accWriteOffs = createLongAccumulator("writeOffs");

  @Variable(name = "Impairments (£k)")
  LongAccumulator accImpairments = createLongAccumulator("impairments");

  @Variable(name = "Debt")
  LongAccumulator accDebt = createLongAccumulator("debt");

  @Variable(name = "Income")
  LongAccumulator accIncome = createLongAccumulator("income");

  @Variable(name = "Mortgages")
  LongAccumulator accMortgages = createLongAccumulator("mortgages");

  @Variable(name = "Assets")
  LongAccumulator accAssets = createLongAccumulator("assets");

  int income = 5000;
  int wealth = 50000;

  @Input(name = "Number of Households")
  long nbHouseholds = 100;

  @Override
  public void setup() {
    Group<Household> householdGroup =
        generateGroup(
            Household.class,
            nbHouseholds,
            house -> {
              house.income = income;
              house.wealth = wealth;
            });

    Group<Bank> bankGroup = generateGroup(Bank.class, 1);

    householdGroup.partitionConnected(bankGroup);

    super.setup();
  }

  @Override
  public void step() {
    super.step();

    run(Household.applyForMortgage(), Bank.processApplication(), Household.takeOutMortgage());

    run(
        Action.create(
            Household.class,
            (Household h) -> {
              h.incomeShock();
              h.earnIncome();
              h.consume();
              h.payMortgage();
            }),
        Action.create(
            Bank.class,
            (Bank b) -> {
              b.accumulateIncome();
              b.processArrears();
              b.calculateImpairments();
              b.clearPaidMortgages();
              b.updateAccumulators();
            }));
  }
}
