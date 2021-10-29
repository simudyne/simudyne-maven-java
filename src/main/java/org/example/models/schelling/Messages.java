package org.example.models.schelling;

import simudyne.core.graph.Message;

public class Messages {
    public static class UnhappyMessage extends Message.Empty {}

    public static class StateMessage extends Message {
        public AgentState state;
    }
}
