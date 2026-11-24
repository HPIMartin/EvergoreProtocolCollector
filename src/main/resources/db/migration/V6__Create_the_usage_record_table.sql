CREATE TABLE `usage_record` (
	`id` INTEGER PRIMARY KEY,
	`account_key` VARCHAR NOT NULL,
	`at_epoch_millis` INTEGER NOT NULL,
	`what` VARCHAR NOT NULL CHECK (`what` IN ('LOGIN', 'PAGE_LOAD', 'LOGOUT', 'VIEW_OVERVIEW', 'VIEW_BANK', 'VIEW_STORAGE',
		'VIEW_EXPLANATION', 'VIEW_ADMIN', 'VIEW_PASSWORD_CHANGE', 'VIEW_NONE', 'LAST_COLUMN_SWITCH', 'OVERVIEW_SORT', 'LEDGER_SORT',
		'LEDGER_PAGE', 'NOTICE_UNDERSTOOD')),
	`detail` VARCHAR
);

CREATE INDEX `usage_record_account_key_at_epoch_millis_idx` ON `usage_record` (`account_key`, `at_epoch_millis`);
