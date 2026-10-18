UPDATE `bankEntries`
SET `timeStampEpochMillis` = CAST(strftime('%s', substr(`timeStamp`, 1, 19)) AS INTEGER) * 1000 + CAST(substr(`timeStamp`, 21, 6) AS INTEGER)
WHERE `timeStampEpochMillis` IS NULL;

UPDATE `storageEntries`
SET `timeStampEpochMillis` = CAST(strftime('%s', substr(`timeStamp`, 1, 19)) AS INTEGER) * 1000 + CAST(substr(`timeStamp`, 21, 6) AS INTEGER)
WHERE `timeStampEpochMillis` IS NULL;
