FROM ubuntu:24.04 AS build
RUN apt-get update && apt-get install -y --no-install-recommends build-essential cmake ca-certificates && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY . .
RUN cmake -S . -B build -DCMAKE_BUILD_TYPE=Release && cmake --build build -j

FROM ubuntu:24.04
WORKDIR /app
ENV TZ=Asia/Shanghai PLC_HOST=127.0.0.1 PLC_RACK=0 PLC_SLOT=1 REDIS_URL=redis://redis:6379 POLL_INTERVAL_MS=500
COPY --from=build /src/build/plc_collector /app/plc_collector
COPY config /app/config
ENTRYPOINT ["/app/plc_collector"]
