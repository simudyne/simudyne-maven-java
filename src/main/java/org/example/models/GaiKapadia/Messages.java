package org.example.models.GaiKapadia;

import simudyne.core.graph.Message;

public class Messages {
    public static class SendConnection extends Message.Empty {}

    public static class StateMessage extends Message {
        public State state;
    }

}