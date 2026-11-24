CREATE TABLE `usage_record` (
	`id` INTEGER,
	`account_key` VARCHAR NOT NULL,
	`at_epoch_millis` INTEGER NOT NULL,
	`what` VARCHAR NOT NULL,
	`detail` VARCHAR
);

CREATE INDEX `usage_record_account_key_at_epoch_millis_idx` ON `usage_record` (`account_key`, `at_epoch_millis`);
