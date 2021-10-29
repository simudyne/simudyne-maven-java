package org.example.models.volatilityModel;

/**
 * Enum to encapsulate toadding strategy behaviour
 */
public enum Behaviour implements Demand {

    FUNDAMENTALIST {
        @Override
        public double demand(double price[]) {
            return phi*(fundamental-price[0]) ;
        }
    },

    CHARTISTS {
        @Override
        public double demand(double price[]) {
            return (chi*(price[0]-price[1]));
        }
    }
}
