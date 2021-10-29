package org.example.models.creditCard;

import simudyne.core.graph.Message;

public class Messages {

    public static class Balance1 extends Message {
        public double balance;
        public double score;
    }

    public static class Balance2 extends Message {
        public double balance;
        public double score;
    }

    public static class CreditCardOffer1 extends Message {
        public double rate;
        public double rewardFactor;
    }

    public static class CreditCardOffer2 extends Message {
        public double rate;
        public double rewardFactor;
    }
}