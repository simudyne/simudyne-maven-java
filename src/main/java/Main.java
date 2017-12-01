import org.example.models.CreditCard;
import providence.simucom.Server;

public class Main {
    public static void main(String[] args) {
        Server.register("Credit Card", CreditCard.class);
        Server.run();
    }
}
