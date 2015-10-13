package cloud.cave.manual;

import cloud.cave.domain.*;
import cloud.cave.server.common.*;
import cloud.cave.service.*;

/**
 * A Null Object Subscription service; all logins are granted.
 *  
 * @author Henrik Baerbak Christensen, Aarhus University.
 * 
 */
public class NullSubscriptionService implements SubscriptionService {
  
  public NullSubscriptionService() {
  }
  
  @Override
  public SubscriptionRecord lookup(String loginName, String password) {
    String playerID = "id-"+loginName;
    SubscriptionRecord record = new SubscriptionRecord(playerID, loginName, "ALL", Region.AALBORG);
    return record;
  }

  @Override
  public void setRestRequester(IRestRequest restRequest) {

  }

  @Override
  public IRestRequest getRestRequester() {
    return null;
  }

  @Override
  public void setSecondsDelay(int secondsDelay) {

  }

  public String toString() {
    return "NullSubscriptionRecord";
  }

  @Override
  public ServerConfiguration getConfiguration() {
    return null;
  }

  @Override
  public void initialize(ServerConfiguration config) {
  }

  @Override
  public void disconnect() {
    // No op
  }
}