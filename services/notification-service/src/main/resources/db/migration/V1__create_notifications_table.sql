CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_notifications_task_id UNIQUE (task_id)
);

CREATE INDEX idx_notifications_owner_id ON notifications (owner_id);
