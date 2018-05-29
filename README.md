# Metrics Paradise

Let's imaging that we have a task of processing and storing large amount of IoT metrics. How would we do that? One approach is to use existing tools and technologies. Other is to design and develop the system from scratch. In these repo I will try both options.

I'll start with the first approach. Usage of well known and common products, such as databases and messaging systems seems simpler because we don't need to write code and also cheaper because we don't have to maintain it later. Also it's easier to find people familiar with these technologies. 

Before choosing right tools for the task we need formalize our requirements. The metric processing system should be:
* Scalable
* Fast
* Self-contained 
* Able to provide aggregation statistic of processed metrics


## First approach
From the requirements it's clear that we need a database to save metrics and retrieve statistics about them. This database should be fast and scalable. Key-value databases is not very helpful here because metrics is a timeseries data that can be handled in a more clever way than just a key-value pairs. 

It's maybe wise to take a look at OLAP databases such as Druid or Clickhouse (later just CH). I've worked with Druid and I didn't like it because it was hard to setup and maintain. I'm curios about CH and I want to try it out for this task.

Clickhouse is advertised as fast and scalable. I've created the Docker Compose environment for testing this storage. CH has built in HTTP server so we can send and run queries from any device with utility such as `curl`. Each coffee machine has it these days, isn't it?

To run load test just execute the command
```
./run-clickhouse-test.sh
```

It will run four nodes of CH with two shards and two replicas. Then script will create distributed table for metrics and start JMeter.

On an average machine it pushes metrics with a rate of 60 RPS.

Scaling mechanism of CH is not very convenient. The cluster nodes are listed in the config file. When it's time to add more instances each config file must be redeployed. Config files can't be redeployed on a working node therefore each node have to be switched off. Common case is to redeploy new configuration on one set of replicas than on another. This method lowers fault tolerance of the system.

Also CH is crashing on malformed queries.

I didn't like the scaling ability of CH and I got excited to build truly scalable storage system by myself.

## Second approach. 

*Building a scalable storage system which one can be proud of.*


















Apply existing tools to solve a problem. For 


Why ClickHouse
* Told to be performant
* Scalable
* Open-sourced
* With rich functionality
* Has SQL-like language


How-to use ClickHouse

To run ClickHouse test docker-compose is needed.

Run command:



It will start four nodes of CH along with? zookeeper, JMeter and Graphite

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