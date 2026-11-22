CREATE TABLE `usage_record` (
	`id` INTEGER,
	`account_key` VARCHAR,
	`at_epoch_millis` INTEGER,
	`what` VARCHAR,
	`detail` VARCHAR
);

CREATE INDEX `usage_record_account_key_at_epoch_millis_idx` ON `usage_record` (`account_key`, `at_epoch_millis`);
