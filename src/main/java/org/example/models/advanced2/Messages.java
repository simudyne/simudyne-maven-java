package org.example.models.advanced2;

import simudyne.core.graph.Message;

public class Messages {
  public static class Arrears extends Message {
    int monthsInArrears;
    int outstandingBalance;
  }

  public static class MortgageCloseAmount extends Message.Integer {}

  public static class MortgageApplication extends Message {
    int amount;
    int income;
    int wealth;
  }

  public static class ApplicationSuccessful extends Message {
    int amount;
    int termInMonths;
    int repayment;
  }

  public static class Payment extends Message {
    int repayment;
    int amount;
  }
}
