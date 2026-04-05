-- Chat module schema (MySQL)
-- spring.jpa.hibernate.ddl-auto=update will also create these tables automatically.

CREATE TABLE IF NOT EXISTS conversations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    admin_id INT NULL,
    status VARCHAR(20) NOT NULL,
    last_message_at DATETIME NULL,
    assigned_at DATETIME NULL,
    last_message_sender_type VARCHAR(10) NULL,
    last_admin_reply_at DATETIME NULL,
    version BIGINT NULL,
    CONSTRAINT fk_conversation_user FOREIGN KEY (user_id) REFERENCES account(id),
    CONSTRAINT fk_conversation_admin FOREIGN KEY (admin_id) REFERENCES account(id)
);

CREATE INDEX IF NOT EXISTS idx_conversation_status_last_message
    ON conversations(status, last_message_at);

CREATE INDEX IF NOT EXISTS idx_conversation_admin_status
    ON conversations(admin_id, status);

CREATE TABLE IF NOT EXISTS messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT NOT NULL,
    sender_id INT NOT NULL,
    sender_type VARCHAR(10) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES account(id)
);

CREATE INDEX IF NOT EXISTS idx_message_conversation_created
    ON messages(conversation_id, created_at);
