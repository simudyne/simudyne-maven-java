package org.example.models.conway;

import simudyne.core.graph.Message;

public class Messages {
  public static class Alive extends Message.Boolean {}

  public static class Start extends Message.Empty {}
}
