echo Setting everything for InVM IPC \(no server\) with test doubles

# === Configure for In memory IPC for the client; server require no config
export SKYCAVE_CLIENTREQUESTHANDLER_IMPLEMENTATION=cloud.cave.doubles.AllTestDoubleClientRequestHandler

# === Dummy config of the server IP endpoint
export SKYCAVE_APPSERVER=localhost:37123

# === Inject test doubles for all delegates (Note IP endpoints are dummies)

# = Subscription service
export SKYCAVE_SUBSCRIPTION_IMPLEMENTATION=cloud.cave.server.service.ServerSubscriptionService
export SKYCAVE_SUBSCRIPTIONSERVER=cavereg.baerbak.com:4567

# = Cave storage
export SKYCAVE_CAVESTORAGE_IMPLEMENTATION=cloud.cave.doubles.FakeCaveStorage
export SKYCAVE_DBSERVER=localhost:27017

# = Rest Requester
export REST_REQUEST_IMPLEMENTATION=cloud.cave.config.socket.RestRequester

# = Weather service
export SKYCAVE_WEATHER_IMPLEMENTATION=cloud.cave.doubles.TestStubWeatherService
export SKYCAVE_WEATHERSERVER=caveweather.baerbak.com:8182
