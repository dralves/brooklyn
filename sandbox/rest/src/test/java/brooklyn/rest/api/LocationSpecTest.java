package brooklyn.rest.api;

import static com.yammer.dropwizard.testing.JsonHelpers.asJson;
import static com.yammer.dropwizard.testing.JsonHelpers.fromJson;
import static com.yammer.dropwizard.testing.JsonHelpers.jsonFixture;
import java.io.IOException;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import org.testng.annotations.Test;

public class LocationSpecTest {

  final LocationSpec locationSpec = LocationSpec.localhost();

  @Test
  public void testSerializeToJSON() throws IOException {
    assertEquals(asJson(locationSpec), jsonFixture("fixtures/location.json"));
  }

  @Test
  public void testDeserializeFromJSON() throws IOException {
    assertEquals(fromJson(jsonFixture("fixtures/location.json"), LocationSpec.class), locationSpec);
  }

  @Test
  public void testDeserializeFromJSONWithNoCredential() throws IOException {
    LocationSpec loaded = fromJson(jsonFixture("fixtures/location-without-credential.json"), LocationSpec.class);

    assertNull(loaded.getConfig().get("credential"));
    assertEquals(loaded.getProvider(), locationSpec.getProvider());
    assertEquals(loaded.getConfig(), locationSpec.getConfig());
  }
}
