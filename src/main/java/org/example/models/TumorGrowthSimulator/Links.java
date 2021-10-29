package org.example.models.TumorGrowthSimulator;

import simudyne.core.graph.Link;

public class Links {
    public static class CellToEnvironmentLink extends Link {
        public double nutrientLevel;
    }

    public static class CellToCellLink extends Link {}

    public static class EnvironmentToEnvironmentLink extends Link {}

}
