ALTER TABLE `bankEntries` ADD COLUMN `timeStampEpochMillis` INTEGER;

ALTER TABLE `storageEntries` ADD COLUMN `timeStampEpochMillis` INTEGER;

CREATE INDEX `bankEntries_avatar_timeStampEpochMillis_idx` ON `bankEntries` (`avatar`, `timeStampEpochMillis`);

CREATE INDEX `storageEntries_avatar_timeStampEpochMillis_idx` ON `storageEntries` (`avatar`, `timeStampEpochMillis`);
