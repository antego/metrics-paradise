# Metrics Paradise

Metrics WANT to be here

Why it's the best place for metrics:

* It's scalable. It scales up, down, right and left. Linearly.
* It's is blazingly fast. Three clients can write to it simultaneously.

How-to use ClickHouse

Database has build in http client. Metrics can be send and extracted via http protocol. Password is set to the 'default' user (see config/users.xml) so it's secured :)
### Create table
Date column is needed by partitoning alghorithm in MergeTree engine
```
echo 'CREATE TABLE Metric (time DateTime, date Date DEFAULT toDate(time), name String, value Float64) ENGINE = MergeTree(date, (time, name), 8192)' | POST 'http://default:password@localhost:8123/'
```
### Send a metric
```
echo -ne '2018-01-01 12:13:45\tmetricname\t13.5\n' | POST 'http://default:password@localhost:8123/?query=INSERT INTO Metric (time, name, value) FORMAT TabSeparated'
```
### Verify data has been saved
```
GET 'http://default:password@localhost:8123/?query=SELECT * FROM Metric'
```
### Drop table
```
echo 'DROP TABLE Metric' | POST 'http://default:password@localhost:8123/'
```

## Sharded and replicated table
```
echo 'CREATE TABLE IF NOT EXISTS MetricReplicated (    
    time DateTime,
    date Date DEFAULT toDate(time),
    name String,
    value Float64
) ENGINE = ReplicatedMergeTree('\''/clickhouse/tables/{shard}/hits'\'', '\''{replica}'\'', date, (time, name), 8192)' | POST 'http://default:password@localhost:8123/'

echo 'CREATE TABLE IF NOT EXISTS MetricSharded AS MetricReplicated
      ENGINE = Distributed(cluster, default, MetricReplicated, rand())' | POST 'http://default:password@localhost:8123/'
      
      
echo -ne '2018-01-01 12:13:45\tmetricname\t13.5\n' | POST 'http://default:password@localhost:8123/?query=INSERT INTO MetricReplicated (time, name, value) FORMAT TabSeparated'

GET 'http://default:password@localhost:8123/?query=SELECT * FROM MetricReplicated'
```

# Second approach

## Performance testing

200 000 metrics has size of 5MB on a file system. It's a size of a database file after Jmeter run with 20k-10thread.jmx test plan.

Single instance consumes 200k metrics from 10 client at a rate of 6k RPS.

Select queries processed at a rate of 5 RPS on a set of 200k metrics.

Grafana dashboard. Credentials are admin/admin 
http://localhost:3000/d/cvQIZ84ik/metrics-paradise?refresh=5s&orgId=1&from=now-30m&to=now&theme=light