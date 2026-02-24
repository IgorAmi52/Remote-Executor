#!/bin/bash
# Compression test - generates and compresses random data
# Tests CPU and I/O performance

SIZE_MB=${1:-10}
TEMP_FILE="/tmp/compress_test_$$"

echo "Generating ${SIZE_MB}MB of random data..."
dd if=/dev/urandom of="$TEMP_FILE" bs=1M count=$SIZE_MB 2>/dev/null

echo "Compressing data with gzip..."
time gzip -c "$TEMP_FILE" > "${TEMP_FILE}.gz"

ORIGINAL_SIZE=$(stat -f%z "$TEMP_FILE" 2>/dev/null || stat -c%s "$TEMP_FILE" 2>/dev/null)
COMPRESSED_SIZE=$(stat -f%z "${TEMP_FILE}.gz" 2>/dev/null || stat -c%s "${TEMP_FILE}.gz" 2>/dev/null)

echo "Original size: $ORIGINAL_SIZE bytes"
echo "Compressed size: $COMPRESSED_SIZE bytes"
echo "Compression ratio: $(echo "scale=2; $COMPRESSED_SIZE * 100 / $ORIGINAL_SIZE" | bc)%"

rm -f "$TEMP_FILE" "${TEMP_FILE}.gz"
echo "Cleanup complete"
