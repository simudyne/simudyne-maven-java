package org.example.models.advanced1;

import simudyne.core.abm.Action;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Input;
import simudyne.core.graph.LongAccumulator;

public class MortgageModel extends AgentBasedModel<GlobalState> {
  LongAccumulator accEquity = createLongAccumulator("equity", "Bank Equity (£)");
  LongAccumulator accAssets = createLongAccumulator("assets", "Assets");

  @Input(name = "Number of Households")
  private long nbHouseholds = 100;

  {
    registerAgentTypes(Bank.class, Household.class);
    registerLinkTypes(Links.BankLink.class);
  }

  @Override
  public void setup() {
    // Create our agent groups
    Group<Household> householdGroup = generateGroup(Household.class, nbHouseholds);
    Group<Bank> bankGroup = generateGroup(Bank.class, 1);

    // Each household is connected to 1 bank
    householdGroup.partitionConnected(bankGroup, Links.BankLink.class);

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
        Action.create(Bank.class, Bank::updateBalanceSheet));
  }
}
