package example.micronaut;

import io.micronaut.http.*;
import io.micronaut.http.annotation.*;

@Controller("/hello") // <1>
public class HelloController {

	private HelloTest test;

	public HelloController(HelloTest test) {
		this.test = test;
		// TODO Auto-generated constructor stub
	}

	@Get // <2>
	@Produces(MediaType.TEXT_PLAIN) // <3>
	public String index() {
		return test.test(); // <4>
	}
}
