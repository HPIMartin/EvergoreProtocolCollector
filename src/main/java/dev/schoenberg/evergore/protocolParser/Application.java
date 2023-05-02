package dev.schoenberg.evergore.protocolParser;

import io.micronaut.runtime.*;

/*
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;

@OpenAPIDefinition(info = @Info(title = "Hello World", version = "0.0", description = "My API", license = @License(name = "Apache 2.0", url = "https://foo.bar"), contact = @Contact(url = "https://gigantic-server.com", name = "Fred", email = "Fred@gigagantic-server.com")))
*/
public class Application {
	public static void main(String[] args) {
		Micronaut.run(Application.class, args);
	}
}