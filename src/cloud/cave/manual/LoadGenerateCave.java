package cloud.cave.manual;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import cloud.cave.client.CaveProxy;
import cloud.cave.config.*;
import cloud.cave.domain.*;
import cloud.cave.ipc.ClientRequestHandler;

/**
 * Manual client side load generator.
 * <p>
 * Based upon two provided parameters: count of players N and
 * iterations I, the load generator starts N concurrent threads
 * each representing a player, and for each player iterates I
 * times over the method 'exploreTheCave' which moves the player,
 * tries to dig a room, and looks a bit around.
 * <p>
 * As the client side PlayerProxy is build for single threaded
 * usage, a global reentrant lock is used (rather coarsely) 
 * to create critical regions around the player proxy.
 * <p>
 * To expose the server for true concurrency, you thus
 * have to start multiple load generators. 
 * <p>
 * As we login anonymous players, the server side cave
 * HAS to be configured with a 'null object' subscription
 * server that allows all (loginName,password) to
 * access the cave.
 * 
 * @author Henrik Baerbak Christensen, Aarhus University.
 * 
 */
public class LoadGenerateCave {

  public static void main(String[] args) {
    System.out.println("*** Load Generator: Generate random user actions on the cave ***");
    if (args.length < 2) {
      System.out.println("Usage: LoadGenerateCave [count player] [iterations]");
      System.exit(-1);
    }
    
    int countPlayer = Integer.parseInt(args[0]);
    int countIteration = Integer.parseInt(args[1]);
    
    LoadGenerateCave loader = new LoadGenerateCave();
    loader.loadWith(countPlayer, countIteration);
  }

  private CaveProxy cave;
  
  public LoadGenerateCave() {
    CaveClientFactory factory; 
    EnvironmentReaderStrategy envReader;
    envReader = new OSEnvironmentReaderStrategy();
    factory = new EnvironmentClientFactory(envReader);
    
    ClientRequestHandler requestHandler = factory.createClientRequestHandler();
    cave = new CaveProxy(requestHandler);
    
    String cfg = cave.describeConfiguration();
    System.out.println("--> Cave initialized; cfg = "+cfg);
    if (! cfg.contains("NullSubscriptionService")) {
      System.out.println("*** ERROR: You can only load generate on cave, if it is configured with the");
      System.out.println("*** ERROR:   NullSubscriptionService. We need to login anonymous players.");
      System.exit(-1);
    }
  }

  Thread[] players = null;
  private void loadWith(int countPlayers, int countIterations) {
    // Create the threads, each represent one player...
    players = new Thread[countPlayers];
    
    // The client side code is not meant for parallel execution
    // and no locking is in place; thus we must use client
    // side locking. Create one global reentrant lock
    final ReentrantLock lock = new ReentrantLock();
    
    // create workers
    System.out.println("--> Creating threads...");
    for(int i = 0; i < countPlayers; i++) {
      // Generate a random player name with very little probability
      // of overlap in case we run multiple instances of this program
      // at the same time
      int randomNumber = (int)(Math.random()*99999);
      String loginName = "Player # " + i + "/" + randomNumber;
      Runnable worker = new SinglePlayerWorker(cave, loginName, countIterations, lock);
      Thread t = new Thread(worker);
      players[i] = t;
    }
    pauseABit();
    
    // start them
    System.out.println("--> Starting threads...");
    for(int i = 0; i < countPlayers; i++) {
      players[i].start();
    }
    
    // join all to main thread
    for(int i = 0; i < countPlayers; i++) {
      try {
        players[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  
  public static void pauseABit() {
    try {
      long sleeptime = 850 + (long) (Math.random()*500L); // 850-1250 ms 
      Thread.sleep(sleeptime);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}

/** Worker thread for a single player, exploring the cave
 * 
 * @author Henrik Baerbak Christensen, Aarhus University.
 *
 */
class SinglePlayerWorker implements Runnable {

  private String loginName;
  private int countIterations;
  private Cave cave;
  private ReentrantLock lock;

  public SinglePlayerWorker(Cave cave, String loginName, 
      int countIterations, ReentrantLock lock) {
    this.cave = cave;
    this.loginName = loginName;
    this.countIterations = countIterations;
    this.lock = lock;
  }

  @Override
  public void run() {
    Login loginResult = null;
    
    // We simulate multiple client but within the
    // SAME program using multiple threads. The
    // client side abstracts are NOT written for
    // concurrency (as clients are expected to have
    // only a single thread). Therefore we must
    // avoid deadlocks/race conditions and other
    // nasty concurrency stuf using 'client-side
    // synchronization': As the methods
    // are not synchronized themselves, we
    // synchronize them from the client
    // side using the global lock given
    lock.lock();
    try {
      loginResult = cave.login( loginName, "no-care");
    } finally {
      lock.unlock();
    }
    Thread.yield();
    pauseABit();

    System.out.println("*** Entering player "+loginName);
    Player p = loginResult.getPlayer();
    for (int i = 0; i < countIterations; i++) {
      exploreTheCave(p, i);
    }
    
    pauseABit();

    lock.lock();
    try {
      cave.logout(p.getID());
    } finally {
      lock.unlock();
    }
    System.out.println("*** Leaving player "+loginName);
  }

  private void exploreTheCave(Player player, int iteration) {
    Direction d; List<Direction> exits; boolean isValid;
    
    lock.lock();
    try {
      // move to a random existing room
      exits = player.getExitSet();
      int n = randomBetween0AndN(exits.size());
      d = exits.get( n );
      isValid = player.move(d);
      assert isValid == true;

      System.out.println("- Player "+ player.getName()+ " moved "+d+"\n  - to '"+player.getShortRoomDescription()+"'");

    } finally {
      lock.unlock();
    }
    pauseABit();
    
    lock.lock();
    try {
      d = null;
      // try to dig a room
      exits = player.getExitSet();
      if (exits.size() < 6) {
        // find a direction without a room
        for (Direction potential : Direction.values()) {
          if (!exits.contains(potential)) {
            d = potential;
          }
        }
      }
      if (d != null) {
        // Dig the room
        isValid = player.digRoom(d,
            "You are in the room made by " + player.getName()
                + " in iteration " + iteration);
        assert isValid == true;
        System.out.println("- Player " + player.getName() + " dug room from "
            + player.getPosition() + " D="+d);
        // and move there to avoid being too much stuck
        assert player.move(d);
      }
    } finally {
      lock.unlock();
    }

    lock.lock();
    try {
      // look around!
      player.getLongRoomDescription(-1);
      player.getExitSet();

      player.getPlayersHere(0);
      player.getPosition();
    } finally {
      lock.unlock();
    }
    Thread.yield();
  }

  private void pauseABit() {
    LoadGenerateCave.pauseABit();
  }

  private int randomBetween0AndN(int n) {
    return (int) (Math.random()*n);
  }


}
