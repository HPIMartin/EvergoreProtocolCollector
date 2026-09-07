CREATE TABLE IF NOT EXISTS `bankEntries` (`id` VARCHAR , `timeStamp` VARCHAR , `avatar` VARCHAR , `amount` INTEGER , `type` VARCHAR , PRIMARY KEY (`id`) );

CREATE TABLE IF NOT EXISTS `storageEntries` (`id` VARCHAR , `timeStamp` VARCHAR , `avatar` VARCHAR , `quantity` INTEGER , `name` VARCHAR , `quality` INTEGER , `type` VARCHAR , PRIMARY KEY (`id`) );

CREATE TABLE IF NOT EXISTS `metaInformation` (`key` VARCHAR , `value` VARCHAR , PRIMARY KEY (`key`) );
