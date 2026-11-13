#!/usr/bin/env bash
# Starts all 6 apps as normal Java programs (no Docker needed).
# Run it from anywhere. First build once:   cd microservices && mvn package -DskipTests
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p logs-local

start() {   # $1 = module name, $2 = its port
  nohup java -jar "$1/target/$1-0.0.1-SNAPSHOT.jar" > "logs-local/$1.log" 2>&1 &
  echo $! > "logs-local/$1.pid"
  echo -n "starting $1 "
  for _ in $(seq 1 60); do
    if curl -s -f "http://localhost:$2/actuator/health/readiness" > /dev/null; then echo "UP"; return; fi
    sleep 2; echo -n "."
  done
  echo "FAILED (look at logs-local/$1.log)"; exit 1
}

# the order matters: settings and phone book first, then the services, then the front door
start config-server 8888
start discovery-server 8761
start movie-service 8081
start payment-service 8083
start booking-service 8082
start api-gateway 8080
echo "All started. Wait about 10 seconds so the services appear in Eureka, then run: scripts/smoke-test.sh"
