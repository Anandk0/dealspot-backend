-- Chat conversations table
CREATE TABLE chat_conversations (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(listing_id, buyer_id)
);

CREATE INDEX idx_chat_conversations_buyer ON chat_conversations(buyer_id);
CREATE INDEX idx_chat_conversations_seller ON chat_conversations(seller_id);

-- Chat messages table
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT,
    voice_url VARCHAR(500),
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_conversation ON chat_messages(conversation_id, created_at);
CREATE INDEX idx_chat_messages_unread ON chat_messages(conversation_id, read, sender_id);
