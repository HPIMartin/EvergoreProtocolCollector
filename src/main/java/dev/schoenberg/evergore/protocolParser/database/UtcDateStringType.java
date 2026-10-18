package dev.schoenberg.evergore.protocolParser.database;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.DateStringType;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;

public final class UtcDateStringType extends DateStringType {
	private static final DateTimeFormatter TO_THE_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final int SECONDS_LENGTH = 19;
	private static final int FRACTION_START = 20;
	private static final UtcDateStringType SINGLETON = new UtcDateStringType();

	private UtcDateStringType() {
		super(SqlType.STRING);
	}

	public static UtcDateStringType getSingleton() {
		return SINGLETON;
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
		Instant moment = ((Date) javaObject).toInstant();
		return TO_THE_SECOND.format(moment.atOffset(UTC)) + "." + String.format(Locale.ROOT, "%06d", moment.get(MILLI_OF_SECOND));
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
		String text = (String) sqlArg;
		long seconds = LocalDateTime.parse(text.substring(0, SECONDS_LENGTH), TO_THE_SECOND).toEpochSecond(UTC);
		long milliseconds = text.length() > FRACTION_START ? Long.parseLong(text.substring(FRACTION_START)) : 0;
		return new Date(seconds * 1000 + milliseconds);
	}
}
