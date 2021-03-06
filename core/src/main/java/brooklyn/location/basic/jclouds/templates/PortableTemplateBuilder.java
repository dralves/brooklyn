package brooklyn.location.basic.jclouds.templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.domain.TemplateBuilderSpec;
import org.jclouds.compute.options.TemplateOptions;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;


public class PortableTemplateBuilder<T extends PortableTemplateBuilder<?>> extends AbstractPortableTemplateBuilder<T> {
    
    ComputeService svc;
    List<TemplateOptions> additionalOptionalOptions = new ArrayList<TemplateOptions>();

    @Override
    public synchronized Template build() {
        if (svc!=null) return newJcloudsTemplate(svc);
        throw new IllegalStateException("Cannot build a portable template until a compute service is attached");
    }
    
    public synchronized ComputeService attachComputeService(ComputeService svc) {
        ComputeService old = this.svc;
        this.svc = svc;
        return old;
    }

    public TemplateBuilder newJcloudsTemplateBuilder(ComputeService svc) {
        TemplateBuilder tb = svc.templateBuilder();
        for (Function<TemplateBuilder,TemplateBuilder> c: commands) {
            tb = c.apply(tb);
        }
        
        tb.options(computeAggregatedOptions(true));
        
        return tb;
    }

    public Template newJcloudsTemplate(ComputeService svc) {
        return newJcloudsTemplateBuilder(svc).build();
    }

    /** Adds template options which are used for building, but not for matching/filtering. 
     * (eg tags added here will be set on any machine created by this template,
     * but will not be required when matching this template to existing machines) */
    public T addOptionalOptions(TemplateOptions options) {
        additionalOptionalOptions.add(options);
        return (T)this;
    }
    
    protected TemplateOptions computeAggregatedOptions(boolean includeOptional) {
        TemplateOptions result = new TemplateOptions();
        if (getOptions()!=null) result = addTemplateOptions(result, getOptions());
        if (includeOptional)
            for (TemplateOptions moreOptions: getAdditionalOptionalOptions()) result = addTemplateOptions(result, moreOptions);
        for (TemplateOptions moreOptions: getAdditionalOptions()) result = addTemplateOptions(result, moreOptions);
        return result;
    }
    
    public List<TemplateOptions> getAdditionalOptionalOptions() {
        return ImmutableList.copyOf(additionalOptionalOptions);
    }
    
    /** like TemplateOptions.copyTo but additive wrt arrays, collections, and maps,
     * putting moreOptions in on top of / at the end of options.
     * currently applies to inboundPorts, tags, and userMetadata. */
    public static TemplateOptions addTemplateOptions(TemplateOptions options, TemplateOptions moreOptions) {
        TemplateOptions result = options.clone();
        moreOptions.copyTo(result);
        
        Set<String> tags = new LinkedHashSet<String>(options.getTags());
        tags.addAll(moreOptions.getTags());
        result.tags(tags);

        Map<String,String> userMetadata = new LinkedHashMap<String,String>(options.getUserMetadata());
        userMetadata.putAll(moreOptions.getUserMetadata());
        result.userMetadata(userMetadata);
        
        Set<Integer> inboundPorts = new TreeSet<Integer>();
        for (int port: options.getInboundPorts()) inboundPorts.add(port);
        for (int port: moreOptions.getInboundPorts()) inboundPorts.add(port);
        int[] inboundPortsArray = new int[inboundPorts.size()];
        int i=0;
        for (Iterator<Integer> portI=inboundPorts.iterator(); portI.hasNext();) {
            inboundPortsArray[i++] = portI.next();
        }
        result.inboundPorts(inboundPortsArray);
        
        return result;
    }

    protected String makeNonTrivialArgumentsString() {
        String s = super.makeNonTrivialArgumentsString();
        TemplateOptions aggr = computeAggregatedOptions(false);
        if (aggr.getInboundPorts().length>0) s = "ports="+Arrays.asList(aggr.getInboundPorts())+(s!=null && s.length()>0 ? ", "+s : "");
        if (!aggr.getUserMetadata().isEmpty()) s = "metadata="+aggr.getUserMetadata()+(s!=null && s.length()>0 ? ", "+s : "");
        if (!aggr.getTags().isEmpty()) s = "tags="+aggr.getTags()+(s!=null && s.length()>0 ? ", "+s : "");
        return s;
    }
    
    @Override
    public TemplateBuilder from(TemplateBuilderSpec spec) {
        TemplateOptions options = new TemplateOptions();
        addOptionalOptions(options);
        TemplateBuilder result = spec.copyTo(this, options);
        return result;
    }

    @Override
    public TemplateBuilder from(String spec) {
        return from(TemplateBuilderSpec.parse(spec));
    }
}
