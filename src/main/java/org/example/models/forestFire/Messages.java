package org.example.models.forestFire;

import simudyne.core.graph.Message;

public class Messages {
    public static class SendContagion extends Message.Empty {}

    public static class StateMessage extends Message {
        public State state;
    }

    public static class AttributeMessage extends Message {
        public int attribute1;
        public int attribute2;
    }

}