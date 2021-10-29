package org.example.models.cda;

import simudyne.core.graph.Message;

public class Messages {

    public static class Bid extends Message {
        public double price;
        public int quantity;
        public Trader owner;
    }

    public static class Ask extends Message {
        public double price;
        public int quantity;
        public Trader owner;
    }

    public static class Transaction extends Message {
        public double bid;
        public double ask;
        public double price;
        public int quantity;
        public Trader bidOwner;
        public Trader askOwner;
    }

    public static class Prediction extends Message {
        public double mean;
        public double scale;
    }

}
