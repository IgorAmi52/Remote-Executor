# Remote Executor Service - Agent Context

## Purpose

This document provides context for AI agents working on this codebase.

## System Overview

A distributed system for executing shell commands on remote executors with resource management. Uses message-driven architecture with AWS SQS (via LocalStack) for communication.

## Architecture Principles

- Clean Architecture: separation of concerns, dependency inversion
- DRY: shared utilities in common module
- Message-driven: loose coupling via SQS queues

## Components

### Controller Module
- Receives command requests from users via REST API
- Sends command messages to Command SQS queue
- Polls Status SQS queue for execution updates
- Persists execution state to PostgreSQL database
- Exposes endpoints for submitting commands and checking execution status

### Executor Module
- Runs as a containerized Java program deployed on EC2 instances
- Polls Command SQS queue for new tasks
- Checks if it has enough CPU resources for the requested task
- Spins up Docker containers with specified CPU limits (`--cpus=N`) to execute commands
- Sends status updates to Status SQS queue

### Common Module
- Generic reusable utilities following DRY principle
- NOT specific to this project - designed to be usable in any repository
- Contains general-purpose helpers only (no domain-specific code)

## Message Flow

1. User submits command to Controller (REST API) with script and CPU requirements
2. Controller persists execution record (status: QUEUED) and sends message to command-queue
3. Executor polls command-queue, checks CPU availability
4. If CPU available: Executor accepts message, sends IN_PROGRESS status to status-queue
5. Executor creates Docker container with CPU limits, executes script
6. Executor sends FINISHED/FAILED status with output to status-queue
7. Controller polls status-queue, updates database
8. User queries execution status via Controller API

## Execution Statuses

- QUEUED: Command received, waiting for executor
- IN_PROGRESS: Executor picked up command, executing
- FINISHED: Command completed successfully
- FAILED: Command failed (non-zero exit code or error)

## Infrastructure

### LocalStack
- Emulates AWS SQS locally for development
- Two queues: command-queue (controller→executor), status-queue (executor→controller)

### Terraform
- Provisions SQS queues on LocalStack
- Infrastructure as code for reproducibility

### Docker Compose
- LocalStack container
- PostgreSQL container
- Note: Controller runs directly on user's machine (not containerized) for direct interaction
- Note: Executor runs on actual EC2, not in docker-compose

### Makefile
- Groups infrastructure provisioning commands
- Build and run targets for development

## Technology Stack

- Language: Java (Maven monorepo)
- Messaging: AWS SQS via LocalStack
- Database: PostgreSQL
- Infrastructure: Terraform, Docker Compose
- Execution isolation: Docker containers with resource limits
