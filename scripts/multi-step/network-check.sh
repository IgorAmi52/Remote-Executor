#!/bin/bash
# Network connectivity test
# Tests network access from container (if allowed)

echo "=== Network Connectivity Test ==="
echo ""

echo "Step 1: Check DNS resolution"
if host google.com > /dev/null 2>&1; then
    echo "DNS resolution: OK"
else
    echo "DNS resolution: FAILED or not available"
fi

echo ""
echo "Step 2: Check outbound connectivity"
if curl -s --connect-timeout 5 -o /dev/null https://httpbin.org/get 2>/dev/null; then
    echo "Outbound HTTPS: OK"
else
    echo "Outbound HTTPS: FAILED or blocked (this may be expected)"
fi

echo ""
echo "Step 3: Check local network"
echo "Network interfaces:"
ip addr 2>/dev/null || ifconfig 2>/dev/null || echo "Network info not available"

echo ""
echo "=== Network Test Complete ==="
