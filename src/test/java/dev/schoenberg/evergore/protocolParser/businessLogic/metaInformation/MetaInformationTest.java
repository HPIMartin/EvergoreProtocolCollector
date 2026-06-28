package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetaInformationTest {

	private TestMetaInformationKey testKey;

	@BeforeEach
	public void setup() {
		testKey = new TestMetaInformationKey();
	}

	@Test
	void usesSerializationMethodFromKey() {
		MetaInformation<Integer> tested = new MetaInformation<>(testKey, 42);
		testKey.serialized = "hello";

		String result = tested.getSerializedValue();

		assertThat(testKey.value).isEqualTo(42);
		assertThat(result).isEqualTo("hello");
	}

	@Test
	void usesDeserializationMethodFromKey() {
		testKey.value = 1337;

		MetaInformation<Integer> result = MetaInformation.fromSerializedValue(testKey, "world");

		assertThat(result.value()).isEqualTo(1337);
		assertThat(testKey.serialized).isEqualTo("world");
	}

	private static class TestMetaInformationKey extends MetaInformationKey<Integer> {
		public Integer value;
		public String serialized;

		TestMetaInformationKey() {
			super("test");
		}

		@Override
		public String serialize(Integer value) {
			this.value = value;
			return serialized;
		}

		@Override
		public Integer deserialize(String raw) {
			serialized = raw;
			return value;
		}
	}
}
