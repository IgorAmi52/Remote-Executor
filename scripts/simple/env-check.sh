#!/bin/bash
# Environment check - prints execution environment details
echo "=== Environment Check ==="
echo "User: $(whoami)"
echo "Working Directory: $(pwd)"
echo "Shell: $SHELL"
echo "PATH: $PATH"
echo ""
echo "=== System Info ==="
uname -a
echo ""
echo "=== Memory ==="
free -h 2>/dev/null || vm_stat 2>/dev/null || echo "Memory info not available"
