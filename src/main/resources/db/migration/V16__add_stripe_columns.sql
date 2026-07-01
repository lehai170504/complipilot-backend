ALTER TABLE organization_subscriptions
ADD COLUMN stripe_customer_id VARCHAR(255),
ADD COLUMN stripe_subscription_id VARCHAR(255),
ADD COLUMN cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_org_sub_stripe_customer_id ON organization_subscriptions(stripe_customer_id);
CREATE INDEX idx_org_sub_stripe_sub_id ON organization_subscriptions(stripe_subscription_id);
