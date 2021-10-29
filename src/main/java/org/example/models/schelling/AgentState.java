package org.example.models.schelling;


public class AgentState {

    public enum Satisfaction {
        HAPPY,
        UNHAPPY
    }

    public enum AgentRace {
        BLUE,
        RED
    }

    public Satisfaction satisfaction;

    public Cell position;

    public double similarityMetric;

    public final AgentRace race;

    public AgentState(AgentRace race) {
        this.race = race;
    }


    public void changePosition(Cell position) {
        this.position = position;
    }

    public void changeSatisfaction(Satisfaction satisfaction) {
        this.satisfaction = satisfaction;
    }

    public void changeSimilarityMetric(double similarityMetric) {
        this.similarityMetric = similarityMetric;
    }

    public void update(AgentState state) {
        similarityMetric = state.similarityMetric;

        position = state.position;
    }
}
