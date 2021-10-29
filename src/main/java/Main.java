
import org.example.models.GaiKapadia.GaiKapadiaModel;
import org.example.models.SimudyneSIR.SimudyneSIR;
import org.example.models.TumorGrowthSimulator.TumorGrowthModel;
import org.example.models.cda.CDAModel;
import org.example.models.creditCard.CreditCardModel;
import org.example.models.forestFire.ForestFireModel;
import org.example.models.mortgage.MortgageModel;
import org.example.models.schelling.SchellingModel;
import org.example.models.tokyo.TokyoModel;
import org.example.models.trading.TradingModel;
import org.example.models.volatilityModel.VolatilityModel;
import simudyne.core.exec.runner.ModelRunner;
import simudyne.core.exec.runner.RunnerBackend;
import simudyne.core.exec.runner.definition.BatchDefinitionsBuilder;
import  simudyne.nexus.Server;

import java.time.Duration;
import java.time.Instant;

public class Main {
  public static void main(String[] args) {
    Instant startTime = Instant.now();
    System.out.println("Start Time " + startTime);

    int runs = !System.getProperty("model.runs").isEmpty() ? Integer.parseInt(System.getProperty("model.runs")) : 1;
    int agents = !System.getProperty("model.agents").isEmpty() ? Integer.parseInt(System.getProperty("model.agents")) : 1000;
    String modelName = !System.getProperty("model.name").isEmpty() ? System.getProperty("model.name") : "GAIKAPDIA";

    try {
      @SuppressWarnings("unchecked")
      RunnerBackend runnerBackend = RunnerBackend.create();
      ModelRunner modelRunner = runnerBackend.forModel(GaiKapadiaModel.class);

      switch (modelName) {
        case "GAIKAPDIA":
        {
          modelRunner = runnerBackend.forModel(GaiKapadiaModel.class);
          break;
        }
        case "SIR":
        {
          modelRunner = runnerBackend.forModel(SimudyneSIR.class);
          break;
        }
        case "FORESTFIRE":
        {
          modelRunner = runnerBackend.forModel(ForestFireModel.class);
          break;
        }
      }
      BatchDefinitionsBuilder runDefinitionBuilder;
      runDefinitionBuilder = BatchDefinitionsBuilder.create().forRuns(runs).forTicks(100).withInput("numAgents", agents);
      modelRunner.forRunDefinitionBuilder(runDefinitionBuilder);
      modelRunner.run().awaitOutput();
      exit(startTime);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void exit(Instant startTime) {
    Instant finishTime = Instant.now();
    long timeElapsed = Duration.between(startTime, finishTime).getSeconds();
    System.out.println("Finish Time " + finishTime);
    System.out.println("Elapsed Time (seconds) " + timeElapsed);
    System.exit(0);
  }
}
