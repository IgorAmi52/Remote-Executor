#!/bin/bash
# Long running script - may timeout depending on executor config
# Useful for testing timeout handling
set -e  # Exit immediately if any command fails
echo "Starting long-running task..."
echo "This will run for 5 minutes (300 seconds)"

for i in $(seq 1 300); do
    echo "Second $i of 300..."
    sleep 1
done

echo "Completed (if not timed out)"
