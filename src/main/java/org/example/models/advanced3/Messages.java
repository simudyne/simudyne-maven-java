package org.example.models.advanced3;

import simudyne.core.graph.Message;

public class Messages {
  public static class Arrears extends Message {
    public int monthsInArrears;
    public int outstandingBalance;
  }

  public static class LoanDefault extends Message.Empty {}

  public static class CloseMortgageAmount extends Message.Integer {}

  public static class MortgageApplication extends Message {
    public int amount;
    public int income;
    public int wealth;
  }

  public static class ApplicationSuccessful extends Message {
    public int amount;
    public int termInMonths;
    public int repayment;
  }

  public static class Payment extends Message {
    int repayment;
    int amount;
  }
}
