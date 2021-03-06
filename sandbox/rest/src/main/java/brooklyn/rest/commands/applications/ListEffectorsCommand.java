package brooklyn.rest.commands.applications;

import brooklyn.rest.api.Application;
import brooklyn.rest.api.EffectorSummary;
import brooklyn.rest.api.EntitySummary;
import brooklyn.rest.commands.BrooklynCommand;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.yammer.dropwizard.json.Json;
import org.apache.commons.cli.CommandLine;

import javax.ws.rs.core.MediaType;
import java.io.PrintStream;
import java.net.URI;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public class ListEffectorsCommand extends BrooklynCommand {

  public ListEffectorsCommand() {
    super("list-effectors", "List all effectors for all entities for a given application");
  }

  @Override
  public String getSyntax() {
    return "[options] <application name>";
  }

  @Override
  protected void run(PrintStream out, PrintStream err, Json json,
                     Client client, CommandLine params) throws Exception {
    checkArgument(params.getArgList().size() >= 1, "Application name is mandatory");

    String name = (String) params.getArgList().get(0);
    Application application = client.resource(uriFor("/v1/applications/" + name))
        .type(MediaType.APPLICATION_JSON_TYPE).get(Application.class);

    queryAllEntities(out, client, application.getLinks().get("entities"));
  }

  private void queryAllEntities(PrintStream out, Client client, URI resource) {
    Set<EntitySummary> entities = client.resource(expandIfRelative(resource))
        .type(MediaType.APPLICATION_JSON_TYPE).get(new GenericType<Set<EntitySummary>>() {
        });

    for (EntitySummary summary : entities) {
      out.println(summary.getLinks().get("self") + " #" + summary.getType());
      queryListOfEffectors(out, client, summary.getLinks().get("effectors"));
      queryAllEntities(out, client, summary.getLinks().get("children"));
    }
  }

  private void queryListOfEffectors(PrintStream out, Client client, URI effectorsUri) {
    Set<EffectorSummary> effectors = client.resource(expandIfRelative(effectorsUri))
        .type(MediaType.APPLICATION_JSON_TYPE).get(new GenericType<Set<EffectorSummary>>() {
        });
    for (EffectorSummary summary : effectors) {
      out.println("\t" + summary.getReturnType() + " " +
          summary.getName() + " " + summary.getParameters());
      out.println("\t" + summary.getLinks().get("self") + "\n");
    }
  }
}
