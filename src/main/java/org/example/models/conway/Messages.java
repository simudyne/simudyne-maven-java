package org.example.models.conway;

public class Messages {
  public static class Neighbour {
    public boolean alive;

    public Neighbour(boolean alive) {
      this.alive = alive;
    }
  }

  public static class Start {}
}
