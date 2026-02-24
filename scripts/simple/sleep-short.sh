#!/bin/bash
# Short sleep test - verifies executor handles waiting tasks
echo "Starting 5 second sleep..."
for i in 1 2 3 4 5; do
    echo "Tick $i"
    sleep 1
done
echo "Sleep completed!"
