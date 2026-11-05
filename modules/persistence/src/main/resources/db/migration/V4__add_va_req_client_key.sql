ALTER TABLE va_req ADD COLUMN client_key VARCHAR(100);

CREATE UNIQUE INDEX uq_va_req_org_client_key ON va_req (org_cd, client_key);
