#!/bin/bash
# Custom exit code test - exits with code 42
# Tests that executor captures non-zero exit codes correctly
set -e  # Exit immediately if any command fails
echo "Testing custom exit codes..."
echo "Will exit with code 42"
exit 42
