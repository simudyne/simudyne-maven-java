import org.example.models.CreditCard;
import org.example.models.conway.GameOfLife;
import simudyne.nexus.Server;

public class Main {
  public static void main(String[] args) {
    Server.register("Credit Card", CreditCard.class);
    Server.register("Game of Life", GameOfLife.class);
    Server.run();
  }
}
