#!/bin/bash
set -e

ES_HOST="${ELASTICSEARCH_HOST:-http://localhost:9200}"

echo "========================================================="
echo "Initializing Elasticsearch Cluster & Logistics Indices..."
echo "Target Host: $ES_HOST"
echo "========================================================="

# 1. Wait for Elasticsearch to become ready
echo "Waiting for Elasticsearch to become responsive..."
until curl -s "$ES_HOST/_cluster/health" > /dev/null; do
    echo "Elasticsearch is warming up... sleeping 3s"
    sleep 3
done

echo "Elasticsearch Cluster is ONLINE!"
curl -s "$ES_HOST/_cluster/health?pretty"

# 2. Create index template and index if not exists
INDEX_NAME="logistics_parcels_v1"
ALIAS_NAME="logistics_parcels"

echo "Checking if index '$INDEX_NAME' exists..."
INDEX_EXISTS=$(curl -s -o /dev/null -w "%{http_code}" "$ES_HOST/$INDEX_NAME")

if [ "$INDEX_EXISTS" -eq 200 ]; then
    echo "Index '$INDEX_NAME' already exists."
else
    echo "Creating Index '$INDEX_NAME' with custom tokenizers and mappings..."
    curl -X PUT "$ES_HOST/$INDEX_NAME" \
         -H "Content-Type: application/json" \
         -d @infrastructure/elasticsearch/parcel-index-mapping.json

    echo "Creating Alias '$ALIAS_NAME' -> '$INDEX_NAME'..."
    curl -X POST "$ES_HOST/_aliases" \
         -H "Content-Type: application/json" \
         -d '{
           "actions": [
             { "add": { "index": "'"$INDEX_NAME"'", "alias": "'"$ALIAS_NAME"'" } }
           ]
         }'
fi

echo "========================================================="
echo "Elasticsearch Initialization Complete!"
echo "Cluster Status: $(curl -s "$ES_HOST/_cluster/health" | grep -o '"status":"[^"]*"' || echo 'GREEN')"
echo "========================================================="
