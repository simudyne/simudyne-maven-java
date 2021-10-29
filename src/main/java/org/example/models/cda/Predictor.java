package org.example.models.cda;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Constant;
import simudyne.core.annotations.Input;

import java.util.Random;

/**
 * Driven by a simple Brownian Motion.
 */

public class Predictor extends Agent<CDAModel.Globals> {

    private double sigma_of_vol = 0.05;
    private double mu_of_vol = 0.0;
    private double sigma_of_mean = 0.2;
    private double mu_of_mean = 0.05;

    Random random;

    double scale = 50.0;

    private double mean = 150.0;

    public static Action<Predictor> inform() {
        return Action.create(Predictor.class, predict -> {

            predict.mean = predict.mean * (1.0 + predict.mu_of_mean / 264.0 +
                    predict.random.nextGaussian() * predict.sigma_of_mean / Math.sqrt(264.0));
            predict.scale = predict.scale * (1.0 + predict.mu_of_vol / 264.0 +
                    predict.random.nextGaussian() * predict.sigma_of_vol / Math.sqrt(264.0));

            predict.getDoubleAccumulator("Belief").add(predict.mean);
            predict.getDoubleAccumulator("BeliefVol").add(predict.scale);

            predict.getLinks(Links.PredictorLink.class).send(Messages.Prediction.class, (b, link) -> {
                b.mean = predict.mean;
                b.scale = predict.scale;
            });
        });
    }
}

