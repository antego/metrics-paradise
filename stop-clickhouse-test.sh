#!/bin/bash

docker stop yandex-tank
docker rm yandex-tank
docker-compose --project-name clickhouse-test down

