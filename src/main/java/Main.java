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
import simudyne.nexus.Server;
import simudyne.core.Model;
import simudyne.core.exec.runner.ModelRunner;
import simudyne.core.exec.runner.RunnerBackend;
import simudyne.core.exec.runner.definition.BatchDefinitionsBuilder;
import simudyne.core.exec.runner.definition.ModelSamplerDefinitionsBuilder;
import simudyne.core.exec.runner.definition.Scenario;
import simudyne.core.exec.runner.definition.ScenarioDefinitionsBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Main {
  public static void main(String[] args) {
    Instant startTime = Instant.now();
    System.out.println("Start Time " + startTime);

    String fallBackMode = "BATCH"; // BATCH, SCENARIO, SAMPLER, CONSOLE, ATOMLOGTEST
    Class fallbackModel = org.example.models.trading.TradingModel.class;
    int runs = (args.length > 1) ? Integer.parseInt(args[0]) : 1;
    long ticks = (args.length > 2) ? Long.parseLong(args[1]) : 100;
    long agents = (args.length > 3) ? Long.parseLong(args[2]) : 1000;

    try {
      @SuppressWarnings("unchecked")
      Class<? extends Model> modelClass = (args.length > 3) ? (Class<? extends Model>) Class.forName(args[3]) : fallbackModel;
      RunnerBackend runnerBackend = RunnerBackend.create();
      ModelRunner modelRunner = runnerBackend.forModel(modelClass);
      String mode = (args.length > 4) ? args[4] : fallBackMode;

      switch (mode) {
        case "BATCH":
        {
          System.out.println("Running in Batch Mode");
          BatchDefinitionsBuilder runDefinitionBuilder;
          if (args.length > 3) {
            runDefinitionBuilder =
                    BatchDefinitionsBuilder.create().forRuns(runs).forTicks(ticks).withInput("numAgents", agents);
          } else {
            runDefinitionBuilder = BatchDefinitionsBuilder.create().forRuns(runs).forTicks(ticks);
          }
          modelRunner.forRunDefinitionBuilder(runDefinitionBuilder);
          modelRunner.run().awaitOutput();
          exit(startTime);
        }
        case "SCENARIO":
        {
          System.out.println("Running in Scenario Mode");
          Scenario runDefinitionBuilder =
                  ScenarioDefinitionsBuilder.create().createScenario("Test").forRuns(runs).forTicks(ticks);
          modelRunner.forRunDefinitionBuilder(runDefinitionBuilder.done());
          modelRunner.run().awaitOutput();
          exit(startTime);
        }
        case "SAMPLER":
        {
          System.out.println("Running in Model Sampler Mode");
          ModelSamplerDefinitionsBuilder runDefinitionBuilder =
                  ModelSamplerDefinitionsBuilder.create().forRuns(runs).forTicks(ticks);
          modelRunner.forRunDefinitionBuilder(runDefinitionBuilder);
          modelRunner.run().awaitOutput();
          exit(startTime);
        }
        case "CONSOLE":
        {
          System.out.println("Running in Console Mode");
          Server.register("Trading Model", TradingModel.class);
          Server.register("Mortgage Model", MortgageModel.class);
          Server.register("Credit Card Model", CreditCardModel.class);
          Server.register("Continuous Double Auction Model", CDAModel.class);
          Server.register("Volatility Model", VolatilityModel.class);
          Server.register("Chain Bankruptcy Model", TokyoModel.class);
          Server.register("S.I.R. Model", SimudyneSIR.class);
          Server.register("Tumor Growth Model", TumorGrowthModel.class);
          Server.register("Schelling Segregation Model", SchellingModel.class);
          Server.register("Forest Fire Model", ForestFireModel.class);
          Server.register("Gai-Kapadia Model", GaiKapadiaModel.class);

          // Start the server.
          Server.run();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public static Map<String, Object> getMap(String str) {
    Map<String, Object> _map = new HashMap<>();
    if(str.isEmpty())
      return _map;

    for(String s0 : str.split(",")) {
      String[] s1 = s0.split("=");
      if(s1.length != 2)
        throw  new IllegalArgumentException("Unknown input format, " + s0);

      _map.put(s1[0], s1[1]);
    }

    return _map;
  }

  public static void exit(Instant startTime) {
    Instant finishTime = Instant.now();
    long timeElapsed = Duration.between(startTime, finishTime).getSeconds();
    System.out.println("Finish Time " + finishTime);
    System.out.println("Elapsed Time (seconds) " + timeElapsed);
    System.exit(0);
  }
}