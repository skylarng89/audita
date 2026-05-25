ALTER TABLE tenants ADD COLUMN subdomain VARCHAR(100);
CREATE UNIQUE INDEX uq_tenants_subdomain ON tenants(subdomain) WHERE subdomain IS NOT NULL;