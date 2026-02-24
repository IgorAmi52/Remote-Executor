#!/bin/bash
# Intentional failure test - exits with code 1
# Tests executor's handling of failed commands

echo "This script will intentionally fail..."
echo "Doing some work first..."
sleep 1
echo "Now failing with exit code 1"
exit 1
