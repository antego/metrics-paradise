#!/bin/bash

chports=(8123 8124 8125 8126)
docker-compose -f clickhouse-compose.yml --project-name clickhouse-test up -d

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

echo "Metrics of performance test can be checked in Graphite: http://localhost:8080/"
echo "Starting JMeter"
docker run --rm -it --network=clickhouse-test_default --name clickhouse-jmeter \
 -v ${PWD}/config/20k-10thread-clickhouse.jmx:/tests/test.jmx justb4/jmeter -n -t "/tests/test.jmx"

 docker-compose -f clickhouse-compose.yml --project-name clickhouse-test down

