package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

public class MetaInformationTest {

	private TestMetaInformationKey testKey;

	@BeforeEach
	public void setup() {
		testKey = new TestMetaInformationKey();
	}

	@Test
	public void usesSerializationMethodFromKey() {
		MetaInformation<Integer> tested = new MetaInformation<>(testKey, 42);
		testKey.serialized = "hello";

		String result = tested.getSerializedValue();

		assertEquals(42, testKey.value);
		assertEquals("hello", result);
	}

	@Test
	public void usesDeserializationMethodFromKey() {
		testKey.value = 1337;

		MetaInformation<Integer> result = MetaInformation.fromSerializedValue(testKey, "world");

		assertEquals(1337, result.value);
		assertEquals("world", testKey.serialized);
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
