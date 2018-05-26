# Metrics Paradise

Metrics WANT to be here

Why it's the best place for metrics:

* It's scalable. It scales up, down, right and left. Linearly.
* It's is blazingly fast. Three clients can write to it simultaneously.

How-to use ClickHouse

Database has its own build in http client. Metrics can be send and extracted via http protocol. Password is set to the 'default' user (see config/users.xml) so it's secured :)
### Create table
Date column is needed by partitoning alghorithm in MergeTree engine
```
echo 'CREATE TABLE metric (time DateTime, date Date, metricName String, metricValue Float64) ENGINE = MergeTree(date, (time, metricName), 8192)' | POST 'http://default:password@localhost:8123/'
```
### Send a metric
```
echo -ne '2018-01-01 12:13:45\t2018-01-01\tmetricname\t13.5\n' | POST 'http://default:password@localhost:8123/?query=INSERT INTO metric FORMAT TabSeparated'
```
### Verify data has been saved
```
GET 'http://default:password@localhost:8123/?query=SELECT * FROM metric'
```
### Drop table
```
echo 'DROP TABLE metric' | POST 'http://default:password@localhost:8123/'
```
