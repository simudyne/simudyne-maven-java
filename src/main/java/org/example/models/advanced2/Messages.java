package org.example.models.advanced2;

public class Messages {
  public static class Arrears {
    public int monthsInArrears;
    public int outstandingBalance;

    public Arrears(int monthsInArrears, int outstandingBalance) {
      this.monthsInArrears = monthsInArrears;
      this.outstandingBalance = outstandingBalance;
    }
  }

  public static class CloseMortgage {
    public int amount;

    public CloseMortgage(int amount) {
      this.amount = amount;
    }
  }

  public static class MortgageApplication {
    public int amount;
    public int income;
    public int wealth;

    public MortgageApplication(int amount, int income, int wealth) {
      this.amount = amount;
      this.income = income;
      this.wealth = wealth;
    }
  }

  public static class ApplicationSuccessful {
    public final int amount;
    public final int termInMonths;
    public final int repayment;

    public ApplicationSuccessful(int amount, int termInMonths, int repayment) {
      this.amount = amount;
      this.termInMonths = termInMonths;
      this.repayment = repayment;
    }
  }

  public static class Payment {
    final int repayment;
    final int amount;

    public Payment(int repayment, int amount) {
      this.repayment = repayment;
      this.amount = amount;
    }
  }
}
