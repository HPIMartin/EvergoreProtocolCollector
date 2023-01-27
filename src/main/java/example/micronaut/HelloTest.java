package example.micronaut;

import jakarta.inject.*;

@Singleton
public class HelloTest {

	public String test() {
		return "Hello World";
	}

}
