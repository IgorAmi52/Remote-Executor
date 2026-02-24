#!/bin/bash
# Missing command test - calls a non-existent command
# Tests executor's handling of command not found errors
set -e  # Exit immediately if any command fails
echo "Attempting to run a non-existent command..."
this_command_does_not_exist_12345
echo "This line should not be reached"
