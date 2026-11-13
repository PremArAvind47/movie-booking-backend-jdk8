#!/usr/bin/env bash
# A quick end-to-end check of the whole system through the gateway.
# Usage: scripts/smoke-test.sh [gateway-url]      (default http://localhost:8080)
# The data is in memory: restart the services for a fresh start before you run it again and again.
BASE="${1:-http://localhost:8080}"
PASS=0
FAIL=0

# call METHOD URL [BODY] [HEADER]   -> sets STATUS and BODY
call() {
  local args=(-s -w $'\n%{http_code}' -X "$1")
  [ -n "${3:-}" ] && args+=(-H "Content-Type: application/json" -d "$3")
  [ -n "${4:-}" ] && args+=(-H "$4")
  local resp
  resp=$(curl "${args[@]}" "$BASE$2")
  STATUS="${resp##*$'\n'}"
  BODY="${resp%$'\n'*}"
}

# expect NAME STATUS TEXT   -> the last call must return STATUS and the body must contain TEXT
expect() {
  if [ "$STATUS" = "$2" ] && echo "$BODY" | grep -q "$3"; then
    PASS=$((PASS + 1)); echo "  PASS  $1"
  else
    FAIL=$((FAIL + 1)); echo "  FAIL  $1  (got HTTP $STATUS: $BODY)"
  fi
}

free_seats() { call GET "/api/v1/movies/$1"; echo "$BODY" | sed -n 's/.*"freeSeats":\([0-9]*\).*/\1/p'; }

echo "Smoke test against $BASE"

# Wait until the gateway can reach the services (they need a few seconds to appear in Eureka).
echo -n "waiting for the system to be ready "
READY=no
for _ in $(seq 1 30); do
  A=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/v1/movies")
  B=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/v1/bookings")
  if [ "$A" = "200" ] && [ "$B" = "200" ]; then READY=yes; break; fi
  echo -n "."; sleep 2
done
if [ "$READY" != yes ]; then echo " NOT READY after 60 s. Are the services running?"; exit 2; fi
echo " ready"

call GET /api/v1/movies;                                             expect "list the movies" 200 '"title":"Batman"'
call POST /api/v1/bookings '{"movieId":1,"seats":1}';                expect "book 1 seat of Avengers" 201 '"status":"CONFIRMED"'
ID=$(echo "$BODY" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
call GET "/api/v1/bookings/$ID";                                     expect "read the booking back" 200 "$ID"

BEFORE=$(free_seats 3)
call POST /api/v1/bookings '{"movieId":3,"seats":5}';                expect "payment declined (5 x 300 = 1500, the limit is 1000)" 402 PAYMENT_DECLINED
AFTER=$(free_seats 3)
if [ -n "$BEFORE" ] && [ "$BEFORE" = "$AFTER" ]; then
  PASS=$((PASS + 1)); echo "  PASS  the seats were released after the declined payment ($BEFORE = $AFTER)"
else
  FAIL=$((FAIL + 1)); echo "  FAIL  seats before $BEFORE, after $AFTER"
fi

# Batman has only 7 seats: book 3 at a time until the seats are gone
GOT409=no
for _ in 1 2 3; do
  call POST /api/v1/bookings '{"movieId":2,"seats":3}'
  if [ "$STATUS" = "409" ]; then GOT409=yes; break; fi
done
if [ "$GOT409" = yes ] && echo "$BODY" | grep -q NOT_ENOUGH_SEATS; then
  PASS=$((PASS + 1)); echo "  PASS  not enough seats gives 409"
else
  FAIL=$((FAIL + 1)); echo "  FAIL  not enough seats (last: HTTP $STATUS $BODY)"
fi

call POST /api/v1/bookings '{"movieId":99,"seats":1}';               expect "unknown movie gives 404" 404 MOVIE_NOT_FOUND
call POST /api/v1/bookings '{"movieId":2,"seats":7}';                expect "too many seats gives 400" 400 TOO_MANY_SEATS
call POST /api/v1/bookings '{"movieId":2}';                          expect "missing seats gives 400" 400 VALIDATION_FAILED
call GET /api/v1/reservations/x;                                     expect "internal reservations API is NOT reachable" 404 ROUTE_NOT_FOUND
call POST /api/v1/payments '{"paymentKey":"x","amount":5}';          expect "internal payments API is NOT reachable" 404 ROUTE_NOT_FOUND

if curl -s -D - -o /dev/null -H "X-Request-Id: smoke-123" "$BASE/api/v1/movies" | grep -qi "x-request-id: smoke-123"; then
  PASS=$((PASS + 1)); echo "  PASS  the request id comes back in the response header"
else
  FAIL=$((FAIL + 1)); echo "  FAIL  the request id header is missing"
fi

echo "Result: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
