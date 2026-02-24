#!/bin/bash
# Pipeline test - chains multiple commands together
# Tests complex command execution

echo "=== Pipeline Test ==="
echo ""

echo "Step 1: Generate data"
sleep 4
DATA=$(seq 1 100)
echo "Generated numbers 1-100"

echo ""
echo "Step 2: Filter even numbers"
EVENS=$(echo "$DATA" | awk '$1 % 2 == 0')
EVEN_COUNT=$(echo "$EVENS" | wc -l | tr -d ' ')
echo "Found $EVEN_COUNT even numbers"

echo ""
echo "Step 3: Calculate sum of even numbers"
SUM=$(echo "$EVENS" | awk '{sum += $1} END {print sum}')
echo "Sum of even numbers: $SUM"

echo ""
echo "Step 4: Find statistics"
MAX=$(echo "$EVENS" | sort -n | tail -1)
MIN=$(echo "$EVENS" | sort -n | head -1)
echo "Min even: $MIN"
echo "Max even: $MAX"

echo ""
echo "=== Pipeline Complete ==="
