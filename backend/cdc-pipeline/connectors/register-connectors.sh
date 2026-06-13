#!/bin/sh
# Registers both Kafka Connect connectors after the Connect worker is healthy.
# Called by the kafka-connect-setup service in docker-compose.

CONNECT_URL="http://kafka-connect:8083"

echo "Registering Debezium MongoDB source connector..."
curl -sf -X POST "${CONNECT_URL}/connectors" \
  -H "Content-Type: application/json" \
  -d @/connectors/debezium-mongodb-connector.json \
  && echo "✓ Debezium connector registered" \
  || echo "✗ Debezium connector registration failed (may already exist)"

echo "Registering Elasticsearch sink connector..."
curl -sf -X POST "${CONNECT_URL}/connectors" \
  -H "Content-Type: application/json" \
  -d @/connectors/elasticsearch-sink-connector.json \
  && echo "✓ Elasticsearch sink connector registered" \
  || echo "✗ Elasticsearch sink connector registration failed (may already exist)"
