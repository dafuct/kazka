--liquibase formatted sql

--changeset kazka:007-user-entitlements-unique-txn splitStatements:true endDelimiter:; runInTransaction:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user_entitlements' AND index_name = 'uk_user_entitlements_original_txn'
--comment Add UNIQUE on user_entitlements.original_transaction_id so two concurrent /iap/verify requests for the same Apple originalTransactionId can't create duplicate rows. MySQL allows multiple NULLs in a UNIQUE constraint, so the column stays nullable for rows created before the txn id is known.
--rollback ALTER TABLE user_entitlements DROP INDEX uk_user_entitlements_original_txn;

ALTER TABLE user_entitlements
    DROP INDEX idx_user_entitlements_original_txn,
    ADD UNIQUE KEY uk_user_entitlements_original_txn (original_transaction_id);
