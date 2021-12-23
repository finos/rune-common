import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VerifyObjectSerialisationTest {

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = RosettaObjectMapper.getNewRosettaObjectMapper();
	}

	@Test
	void verifyObjectSerialisationTest() throws JsonProcessingException {
		String actual = objectMapper.writeValueAsString(new Foo("xmas", new Bar(999)));
		System.out.println(actual);
	}

	static class Foo implements FooInterface {
		String x;
		Bar bar;

		@JsonCreator
		public Foo(@JsonProperty("x") String x, @JsonProperty("bar") Bar bar) {
			this.x = x;
			this.bar = bar;
		}

		public String getX() {
			return x;
		}

		public Bar getBar() {
			return bar;
		}
	}

	static class Bar {
		int y;

		@JsonCreator
		public Bar(@JsonProperty("y") int y) {
			this.y = y;
		}

		public int getY() {
			return y;
		}
	}

	interface FooInterface {
		default Class<? extends FooInterface> getType() {
			return FooInterface.class;
		}
	}
}
