#!/bin/bash
# CPU burn test - intensive calculation loop
# Useful for testing CPU resource limits

ITERATIONS=${1:-1000000}
echo "Running $ITERATIONS calculation iterations..."
sleep 3

start_time=$(date +%s.%N)

sum=0
for ((i=1; i<=ITERATIONS; i++)); do
    sum=$((sum + i * i))
    if ((i % 100000 == 0)); then
        echo "Progress: $i / $ITERATIONS"
    fi
done

end_time=$(date +%s.%N)
duration=$(echo "$end_time - $start_time" | bc)

echo "Completed!"
echo "Final sum: $sum"
echo "Duration: ${duration}s"
