package org.example.models.advanced1;

import simudyne.core.abm.Action;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.Variable;
import simudyne.core.graph.LongAccumulator;

public class MortgageModel extends AgentBasedModel<GlobalState> {
  @Variable(name = "Bank Equity (£)")
  LongAccumulator accEquity = createLongAccumulator("equity");

  @Variable(name = "assets")
  LongAccumulator accAssets = createLongAccumulator("assets");

  @Input(name = "Number of Households")
  long nbHouseholds = 100;

  @Override
  public void setup() {
    // Create our agent groups
    Group<Household> householdGroup = generateGroup(Household.class, nbHouseholds);
    Group<Bank> bankGroup = generateGroup(Bank.class, 1);

    // Each household is connected to 1 bank
    householdGroup.partitionConnected(bankGroup);

    super.setup();
  }

  @Override
  public void step() {
    super.step();

    run(
        Action.create(
            Household.class,
            household -> {
              household.earnIncome();
              household.consume();
              household.payMortgage();
            }),
        Action.create(
            Bank.class,
            bank -> {
              bank.updateBalanceSheet();
            }));
  }
}
