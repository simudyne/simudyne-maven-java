package org.example.models.volatilityModel;

/**
 * Demand interface to be implemented by behaviour
 */
public interface Demand {

    double phi = 1.00;
    double chi = 1.20;
    double fundamental = 0;

    /**
     * Demand method takes in some random noise from the agent and the current price time series and
     * returns the value that agent demands.
     * @param price
     * @return
     */
    double demand(double price[]);
}