package org.example.models.creditCard;

import simudyne.core.Model;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.ModelSettings;
import simudyne.core.annotations.Variable;

@ModelSettings(timeUnit = "MONTHS", macroStep = 12)
public class CreditCard implements Model {

  @Input(name = "Spending")
  public boolean isSpending = true;

  @Input(name = "Spending Amount")
  public float spending = 250f;

  @Input(name = "Repayment Amount")
  public float repayment = 200f;

  @Input(name = "Interest Rate")
  public float interest = 0.03f;

  @Variable(name = "Balance", initializable = true)
  public float balance = 400;

  @Variable(name = "Interest Charge")
  public float interest_charge() {
    return interest * balance;
  }

  @Variable(name = "Balance Additions")
  public float balance_additions() {
    return interest_charge() + (isSpending ? spending : 0);
  }

  // Model Interface Methods

  public void step() {
    balance += balance_additions() - repayment;

    if (balance < 0) {
      balance = 0;
    }
  }
}
