package brooklyn.rest.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import static com.yammer.dropwizard.testing.JsonHelpers.asJson;
import static com.yammer.dropwizard.testing.JsonHelpers.fromJson;
import static com.yammer.dropwizard.testing.JsonHelpers.jsonFixture;
import java.io.IOException;
import java.net.URI;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

public class EffectorSummaryTest {

  final EffectorSummary effectorSummary = new EffectorSummary(
      "stop",
      "Effector description",
      "void",
      ImmutableSet.<EffectorSummary.ParameterSummary>of(),
      ImmutableMap.of(
          "self", URI.create("/v1/applications/redis-app/entities/redis-ent/effectors/stop")
      )
  );

  @Test
  public void testSerializeToJSON() throws IOException {
    assertEquals(asJson(effectorSummary), jsonFixture("fixtures/effector-summary.json"));
  }

  @Test
  public void testDeserializeFromJSON() throws IOException {
    assertEquals(fromJson(jsonFixture("fixtures/effector-summary.json"), EffectorSummary.class), effectorSummary);
  }
}
