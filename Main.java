import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main
{

    public static boolean gVerbose = false;

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws Exception {
        // Parse parameters
        boolean lCreateServer = false;
        String lLoadFilename = "SouthEmissions.in"; //default environment

        //scan input arguments
        for (int i = 0; i < args.length; ++i) {
            String param = args[i];

            if (param.equals("server") || param.equals("s")) { //questo codice deve girare come server, default è client
                lCreateServer = true;
            } else if (param.equals("verbose") || param.equals("v")) { //per stampare informazioni aggiuntive
                gVerbose = true;
            } else if (param.equals("load") || param.equals("l")) { //per environments diversi da quello di default
                ++i;
                if (i < args.length)
                    lLoadFilename = args[i]; //mette nuovo environment qua dentro
                else
                {
                    System.err.println("Observations file must be given as an argument");
                    System.exit(-1);
                }
            } else {
                System.err.println("Unknown parameter: '" + args[i] + "'");
                System.exit(-1);
            }
        }

        /**
         * Start the program either as a server or as a client
         */
        if (lCreateServer) //se sono server
        {
            // Create a server
            GameServer lGameServer = new GameServer(
                    new BufferedReader(new InputStreamReader(System.in)),
                    System.out);

            if (lLoadFilename != null)
            {
                if (gVerbose)
                    System.err.println("Loading '" + lLoadFilename + "'");
                lGameServer.load(new FileReader(lLoadFilename));
            }

            // Run the server
            lGameServer.run();
        }
        else //se sono client
        {
            // Create the player
            Player lPlayer = new Player();

            // Create a client with the player
            Client lClient = new Client(
                    lPlayer,
                    new BufferedReader(new InputStreamReader(System.in)),
                    System.out);

            // Run the client
            lClient.run();
        }
    }
}
