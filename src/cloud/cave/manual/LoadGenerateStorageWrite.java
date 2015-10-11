package cloud.cave.manual;

import cloud.cave.common.CaveStorageException;
import cloud.cave.config.*;
import cloud.cave.domain.*;
import cloud.cave.server.*;

/**
 * Manual load generator. Generates 10.000 player.addRoom() requests to the
 * Cave, thereby forcing a large number of writes to the underlying storage
 * system.
 * 
 * @author Henrik Baerbak Christensen, Aarhus University.
 * 
 */
public class LoadGenerateStorageWrite {
  public static void main(String[] args) throws InterruptedException {
    
    System.out.println("*** Load Generator: Generate writes in the storage ***");
    
    CaveServerFactory factory; 
    EnvironmentReaderStrategy envReader;
    envReader = new OSEnvironmentReaderStrategy();
    factory = new EnvironmentServerFactory(envReader);
    
    // Create the server side cave instance
    Cave cave = new StandardServerCave(factory);
    
    System.out.println("--> Cave initialized; cfg = "+cave.describeConfiguration());

    // Login reserved user
    Login result = cave.login("reserved_aarskort", "cloudarch");
    System.out.println("--> login result: "+result);
    
    // assume it went ok
    Player player = result.getPlayer();

    System.out.println("--> player logged into cave");
    
    System.out.println("** Initialized, will start digging DOWN ***");
    
    // Generate load
    final int max = 10000;
    boolean dig = true;
    boolean wentOk;
    for (int i = 0; i < max; i++) {
      if (i%100 == 0) { System.out.print("."); }
      if (i%1000 == 0) { System.out.println(); }

      try {
        if (dig) {
          String roomDescription = "This is room no. " + i;
          wentOk = player.digRoom(Direction.DOWN, roomDescription);
          if (!wentOk) {
            System.out.println("ERROR: The cave is not empty, failed on digging room at position: " + player.getPosition());
            System.exit(-1); // Fail fast...
          }
        }
      } catch (CaveStorageException e) {
        System.out.println("Cave storage currently unavailable, retrying in ~10 secs");

        i--;
        Thread.sleep(10000);
        continue;
      }

      // move down then
      try {
        player.move(Direction.DOWN);
      } catch (CaveStorageException e) {
        System.out.println("Cave storage currently unavailable, retrying in ~10 secs");

        dig = false;
        i--;
        Thread.sleep(10000);
        continue;
      }
    }
    System.out.println();
    System.out.println("*** Done. Remember to erase DB manually before attempting a new run. ***");

  }

}
