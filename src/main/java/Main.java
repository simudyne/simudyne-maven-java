import org.example.models.conway.GameOfLife;
import org.example.models.trading.TradingModel;
import simudyne.nexus.Server;

public class Main {
  public static void main(String[] args) {
    // Server.register("My Credit Card", CreditCard.class);
    Server.register("Game of Life", GameOfLife.class);
    Server.register("Trading Model", TradingModel.class);
    Server.register(
        "Mortgage Tutorial: Skeleton", org.example.models.advanced1.MortgageModel.class);
    Server.register(
        "Mortgage Tutorial: Intermediate", org.example.models.advanced2.MortgageModel.class);
    Server.register("Mortgage Tutorial: Final", org.example.models.advanced3.MortgageModel.class);
    Server.run();
  }
}
