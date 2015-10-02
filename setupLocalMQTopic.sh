#!/bin/bash
echo Setting everything for socket based connection on LocalHost with test doubles

# === Configure for socket communication on client and app server side
export SKYCAVE_CLIENTREQUESTHANDLER_IMPLEMENTATION=cloud.cave.config.socket.RabbitTopicRequestHandler
export SKYCAVE_REACTOR_IMPLEMENTATION=cloud.cave.config.socket.RabbitTopicReactor

# === Configure for server to run on localhost
export SKYCAVE_APPSERVER=localhost:5672

# = Subscription service
export SKYCAVE_SUBSCRIPTION_IMPLEMENTATION=cloud.cave.server.service.ServerSubscriptionService
export SKYCAVE_SUBSCRIPTIONSERVER=cavereg.baerbak.com:4567

# = Cave storage
export SKYCAVE_CAVESTORAGE_IMPLEMENTATION=cloud.cave.doubles.FakeCaveStorage
export SKYCAVE_DBSERVER=localhost:27017

# = Rest Requester
export REST_REQUEST_IMPLEMENTATION=cloud.cave.doubles.SubscriptionServiceRequestFake

# = Weather service
export SKYCAVE_WEATHER_IMPLEMENTATION=cloud.cave.server.service.ServerWeatherService
export SKYCAVE_WEATHERSERVER=caveweather.baerbak.com:8182



