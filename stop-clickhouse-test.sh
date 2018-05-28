#!/bin/bash

docker stop yandex-tank
docker-compose -f clickhouse-compose.yml --project-name clickhouse-test down

