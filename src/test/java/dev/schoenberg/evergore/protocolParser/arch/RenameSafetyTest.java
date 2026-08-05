package dev.schoenberg.evergore.protocolParser.arch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

@AnalyzeClasses(packages = "dev.schoenberg.evergore.protocolParser", importOptions = ImportOption.DoNotIncludeTests.class)
class RenameSafetyTest {
	private static final String WIRE_PACKAGE = "..rest.controller.api.wire..";

	@ArchTest
	static final ArchRule everyWireFieldPinsItsJsonName = fields()
			.that()
			.areDeclaredInClassesThat()
			.resideInAPackage(WIRE_PACKAGE)
			.and()
			.areNotStatic()
			.should()
			.beAnnotatedWith(JsonProperty.class)
			.because("renaming a record component must never change the published JSON contract");

	@ArchTest
	static final ArchRule everyDatabaseFieldPinsItsColumnName = fields()
			.that()
			.areAnnotatedWith(DatabaseField.class)
			.should(nameTheirColumn())
			.because("renaming a field must never change the database schema");

	@ArchTest
	static final ArchRule everyDatabaseEntityPinsItsTableName = classes()
			.that()
			.areAnnotatedWith(DatabaseTable.class)
			.should(nameTheirTable())
			.because("renaming an entity must never change the database schema");

	private static ArchCondition<JavaField> nameTheirColumn() {
		return new ArchCondition<>("declare a non-empty columnName") {
			@Override
			public void check(JavaField field, ConditionEvents events) {
				String columnName = field.getAnnotationOfType(DatabaseField.class).columnName();
				events.add(new SimpleConditionEvent(field, !columnName.isEmpty(), field.getFullName() + " declares columnName \"" + columnName + "\""));
			}
		};
	}

	private static ArchCondition<JavaClass> nameTheirTable() {
		return new ArchCondition<>("declare a non-empty tableName") {
			@Override
			public void check(JavaClass entity, ConditionEvents events) {
				String tableName = entity.getAnnotationOfType(DatabaseTable.class).tableName();
				events.add(new SimpleConditionEvent(entity, !tableName.isEmpty(), entity.getName() + " declares tableName \"" + tableName + "\""));
			}
		};
	}
}
