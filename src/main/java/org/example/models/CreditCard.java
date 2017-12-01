package org.example.models;

import providence.simucore.Model;
import providence.simucore.annotations.*;

@ModelSettings(timeUnit = "MONTHS")
public class CreditCard implements Model {
    @Constant(name = "Initial Balance")
    public long initial_balance = 400;
    @Constant
    public long credit_limit = 0;

    @Input
    public boolean isSpending = true;

    @Input(name = "Spending")
    public float spending = 250f;
    @Input(name = "Repayment amount")
    public float repayment = 200f;
    @Input(name = "Interest Rate")
    public float interest = 0.03f;

    @Variable(name = "Balance")
    public float balance = 0;

    @Variable(name = "Interest Charge")
    public float interest_charge() {
        return interest * balance;
    }

    @Variable(name = "Balance Additions")
    public float balance_additions() {
        return interest_charge() + (isSpending ? spending : 0);
    }

    // SDModel Interface Methods

    public void setup() {
        balance = initial_balance;
    }

    public void calculate() {
        balance += balance_additions() - repayment;

        if (balance < 0) {
            balance = 0;
        }
    }
}
