CREATE TABLE `account` (
	`id` VARCHAR NOT NULL,
	`avatar` VARCHAR NOT NULL,
	`name_key` VARCHAR NOT NULL,
	`password_hash` VARCHAR NOT NULL,
	`password_state` VARCHAR NOT NULL CHECK (`password_state` IN ('ISSUED', 'CHOSEN')),
	`role` VARCHAR NOT NULL DEFAULT 'MEMBER' CHECK (`role` IN ('MEMBER', 'ADMIN')),
	`last_login_epoch_millis` INTEGER,
	PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX `account_name_key_idx` ON `account` (`name_key`);
