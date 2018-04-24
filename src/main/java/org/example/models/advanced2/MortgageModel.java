package org.example.models.advanced2;

import simudyne.core.abm.Action;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;

@ModelSettings(macroStep = 120)
public class MortgageModel extends AgentBasedModel<GlobalState> {

  {
    createLongAccumulator("equity", "Bank Equity (£)");
    createLongAccumulator("badLoans", "Bad Loans");
    createLongAccumulator("writeOffs", "Write-offs");
    createLongAccumulator("impairments", "Impairments (£k)");
    createLongAccumulator("debt", "Debt");
    createLongAccumulator("income", "Income");
    createLongAccumulator("mortgages", "Mortgages");
    createLongAccumulator("assets", "Assets");
  }

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
