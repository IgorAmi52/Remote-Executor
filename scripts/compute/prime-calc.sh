#!/bin/bash
# Prime number calculation - CPU intensive task
# Finds all primes up to a limit using trial division

LIMIT=${1:-10000}
echo "Finding prime numbers up to $LIMIT..."

count=0
for ((n=2; n<=LIMIT; n++)); do
    is_prime=1
    for ((i=2; i*i<=n; i++)); do
        if ((n % i == 0)); then
            is_prime=0
            break
        fi
    done
    if ((is_prime == 1)); then
        ((count++))
    fi
done

echo "Found $count prime numbers up to $LIMIT"
