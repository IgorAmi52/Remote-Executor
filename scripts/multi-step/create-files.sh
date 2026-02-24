#!/bin/bash
# File creation test - creates temp files and cleans up
# Tests filesystem operations in container

WORK_DIR="/tmp/executor_test_$$"
mkdir -p "$WORK_DIR"
echo "Created work directory: $WORK_DIR"

echo "Creating test files..."
for i in 1 2 3 4 5; do
    echo "Content of file $i - created at $(date)" > "$WORK_DIR/file_$i.txt"
    echo "Created file_$i.txt"
done

echo ""
echo "Listing created files:"
ls -la "$WORK_DIR"

echo ""
echo "Reading file contents:"
cat "$WORK_DIR"/*.txt

echo ""
echo "Cleaning up..."
rm -rf "$WORK_DIR"
echo "Done!"
