#!/bin/bash

chports=(8123 8124 8125 8126)
docker-compose --project-name clickhouse-test up -d

# Create tables on each replica
for PORT in "${chports[@]}"; do
	for i in {1..5}; do # 5 retries
		echo "Creating sharded table on host localhost:$PORT. Attempt #$i"
		echo 'CREATE TABLE IF NOT EXISTS MetricReplicated (    
			time DateTime,
			date Date DEFAULT toDate(time),
			name String,
			value Float64
		) ENGINE = ReplicatedMergeTree('\''/clickhouse/tables/{shard}/hits'\'', '\''{replica}'\'', date, (time, name), 8192)' \
		 | POST "http://default:password@localhost:${PORT}/" 2>/dev/null && break
		sleep 2
	done
done

echo "Creating distributed table"
echo 'CREATE TABLE IF NOT EXISTS MetricSharded AS MetricReplicated
      ENGINE = Distributed(cluster, default, MetricReplicated, rand())' \
      | POST "http://default:password@localhost:${chports[1]}/"
      
echo "Starting Yandex Tank"
docker run --rm -d --network=clickhouse-test_default --name yandex-tank \
 -v ${PWD}/config/tank.ini:/tank.ini -v ${PWD}/logs:/logs --entrypoint "/usr/bin/yandex-tank" \
 gtrafimenkov/yandex-tank -c /tank.ini >/dev/null

echo "You can check statistics in Graphite: http://localhost:8080/"
