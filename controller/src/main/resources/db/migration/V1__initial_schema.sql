CREATE TABLE IF NOT EXISTS executions (
    id TEXT PRIMARY KEY,
    script_name TEXT NOT NULL,
    script_content TEXT NOT NULL,
    required_cpus INTEGER NOT NULL,
    current_status TEXT NOT NULL,
    exit_code INTEGER,
    output TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS status_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    execution_id TEXT NOT NULL,
    status TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    FOREIGN KEY (execution_id) REFERENCES executions(id)
);

CREATE INDEX IF NOT EXISTS idx_execution_id ON status_history(execution_id);
CREATE INDEX IF NOT EXISTS idx_status_history_timestamp ON status_history(timestamp);
CREATE INDEX IF NOT EXISTS idx_executions_created_at ON executions(created_at);
