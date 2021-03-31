package org.example.models.schelling;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.Group;
import simudyne.core.annotations.ModelSettings;

@ModelSettings(macroStep = 100)
public class SchellingModel extends AgentBasedModel<Environment> {

    {
        registerAgentTypes(SchellingAgent.class, BlueAgent.class, RedAgent.class);
        createLongAccumulator("numUnhappy", "Number of unhappy agents");
        createDoubleAccumulator("avSimilarity", "Average Similarity");
    }

    @Override
    public void setup() {
        System.out.println(getGlobals().nbBlue + " " + getGlobals().nbRed + " " + getGlobals().nbEmpty);

        Group<BlueAgent> blueAgentGroup = generateGroup(BlueAgent.class, getGlobals().nbBlue,
                blueAgent -> {
                    blueAgent.similarityThreshold = getGlobals().similarityThreshold;
                });
        Group<RedAgent> redAgentGroup = generateGroup(RedAgent.class, getGlobals().nbRed,
                redAgent -> {
                    redAgent.similarityThreshold = getGlobals().similarityThreshold;
                });

        super.setup();
    }

    @Override
    public void step() {

        if (getContext().getTick() == 0) {
            run(SchellingAgent.register());
            getGlobals().initPositions();
        }
        else {
            run(SchellingAgent.checkSatisfaction());
            run(SchellingAgent.move());
        }

        getGlobals().calculateSimilarityMetrics();
        run(SchellingAgent.updateState());

    }
}
