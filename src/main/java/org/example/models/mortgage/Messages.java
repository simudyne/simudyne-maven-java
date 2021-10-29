package org.example.models.mortgage;

import simudyne.core.graph.Message;

public class Messages {

  public static class Arrears extends Message {
    public int monthsInArrears;
    public int outstandingBalance;
    public int creditScore;
  }

  public static class CloseMortgage extends Message {
    public int amount;

  }

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
    public int repayment;
    public int amount;
    public int outstandingBalance;
    public int creditScore;
  }

  public static class Default extends Message.Boolean {

  }

  public static class Start extends Message.Empty {

  }
}

