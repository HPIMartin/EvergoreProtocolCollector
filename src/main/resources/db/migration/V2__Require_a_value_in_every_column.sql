CREATE TABLE `bankEntries_strict` (
	`id` VARCHAR NOT NULL,
	`timeStamp` VARCHAR NOT NULL,
	`avatar` VARCHAR NOT NULL,
	`amount` INTEGER NOT NULL,
	`type` VARCHAR NOT NULL,
	PRIMARY KEY (`id`)
);

INSERT INTO `bankEntries_strict` SELECT `id`, `timeStamp`, `avatar`, `amount`, `type` FROM `bankEntries`;

DROP TABLE `bankEntries`;

ALTER TABLE `bankEntries_strict` RENAME TO `bankEntries`;

CREATE TABLE `storageEntries_strict` (
	`id` VARCHAR NOT NULL,
	`timeStamp` VARCHAR NOT NULL,
	`avatar` VARCHAR NOT NULL,
	`quantity` INTEGER NOT NULL,
	`name` VARCHAR NOT NULL,
	`quality` INTEGER NOT NULL,
	`type` VARCHAR NOT NULL,
	PRIMARY KEY (`id`)
);

INSERT INTO `storageEntries_strict` SELECT `id`, `timeStamp`, `avatar`, `quantity`, `name`, `quality`, `type` FROM `storageEntries`;

DROP TABLE `storageEntries`;

ALTER TABLE `storageEntries_strict` RENAME TO `storageEntries`;

CREATE TABLE `metaInformation_strict` (
	`key` VARCHAR NOT NULL,
	`value` VARCHAR NOT NULL,
	PRIMARY KEY (`key`)
);

INSERT INTO `metaInformation_strict` SELECT `key`, `value` FROM `metaInformation`;

DROP TABLE `metaInformation`;

ALTER TABLE `metaInformation_strict` RENAME TO `metaInformation`;
