package org.broadinstitute.gatk.tools.walkers.variantqc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.broadinstitute.gatk.engine.arguments.StandardVariantContextInputArgumentCollection;
import org.broadinstitute.gatk.engine.walkers.*;
import org.broadinstitute.gatk.tools.walkers.varianteval.VariantEval;
import org.broadinstitute.gatk.utils.classloader.JVMUtils;
import org.broadinstitute.gatk.utils.commandline.ArgumentCollection;
import org.broadinstitute.gatk.utils.commandline.Output;
import org.broadinstitute.gatk.utils.contexts.AlignmentContext;
import org.broadinstitute.gatk.utils.contexts.ReferenceContext;
import org.broadinstitute.gatk.utils.refdata.RefMetaDataTracker;
import org.broadinstitute.gatk.utils.report.GATKReportTable;
import org.broadinstitute.gatk.utils.report.GATKReportVersion;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bimber on 5/4/2017.
 */
@Reference(window=@Window(start=-50, stop=50))
@PartitionBy(PartitionType.NONE)
public class VariantQC extends RodWalker<Integer, Integer> implements TreeReducible<Integer> {
    private VariantEval sampleStratifiedWalker;
    private VariantEval locusStratifiedWalker;

    @ArgumentCollection
    protected StandardVariantContextInputArgumentCollection variantCollection = new StandardVariantContextInputArgumentCollection();

    @Output
    protected PrintStream out;

    protected ByteArrayOutputStream bao1 = new ByteArrayOutputStream();
    protected ByteArrayOutputStream bao2 = new ByteArrayOutputStream();

    @Override
    public void initialize() {
        super.initialize();

        //configure the child walkers
        sampleStratifiedWalker = new VariantEval();
        locusStratifiedWalker = new VariantEval();

        //manually set arguments
        try
        {
            Field stratificationsToUseField = VariantEval.class.getField("STRATIFICATIONS_TO_USE");
            JVMUtils.setFieldValue(stratificationsToUseField, sampleStratifiedWalker, new String[]{"Sample"});
            JVMUtils.setFieldValue(stratificationsToUseField, locusStratifiedWalker, new String[]{"Contig"});

            Field evalField = VariantEval.class.getField("eval");
            JVMUtils.setFieldValue(evalField, sampleStratifiedWalker, Arrays.asList(variantCollection.variants));
            JVMUtils.setFieldValue(evalField, locusStratifiedWalker, Arrays.asList(variantCollection.variants));

            Field outField = VariantEval.class.getField("out");
            JVMUtils.setFieldValue(evalField, outField, new PrintWriter(bao1));
            JVMUtils.setFieldValue(evalField, outField, new PrintWriter(bao2));
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }

        //initialize
        sampleStratifiedWalker.initialize();
        locusStratifiedWalker.initialize();
    }

    @Override
    public Integer treeReduce(Integer lhs, Integer rhs) {
        sampleStratifiedWalker.reduce(lhs, rhs);
        locusStratifiedWalker.reduce(lhs, rhs);

        return null;
    }

    @Override
    public Integer map(RefMetaDataTracker tracker, ReferenceContext ref, AlignmentContext context) {
        sampleStratifiedWalker.map(tracker, ref, context);
        locusStratifiedWalker.map(tracker, ref, context);

        return null;
    }

    @Override
    public Integer reduceInit() {
        sampleStratifiedWalker.reduceInit();
        locusStratifiedWalker.reduceInit();

        return null;
    }

    @Override
    public Integer reduce(Integer value, Integer sum) {
        sampleStratifiedWalker.reduce(value, sum);
        locusStratifiedWalker.reduce(value, sum);

        return null;
    }

    @Override
    public void onTraversalDone(Integer result) {
        super.onTraversalDone(result);

        sampleStratifiedWalker.onTraversalDone(result);
        locusStratifiedWalker.onTraversalDone(result);

        //do our actual work here
        GATKReportTable sampleTable = new GATKReportTable(new BufferedReader(new StringReader(new String(bao1.toByteArray()))), GATKReportVersion.V1_1);
        GATKReportTable locusTable = new GATKReportTable(new BufferedReader(new StringReader(new String(bao2.toByteArray()))), GATKReportVersion.V1_1);

        //make classes like this to translate from the GATKReportTable into the config object we need in our HTML
        List<JsonTranslator> translators = new ArrayList<>();
        translators.add(new JsonTranslator(sampleTable, "Plot1", JsonTranslator.PlotType.bar_graph));

        JsonArray sections = new JsonArray();
        for (JsonTranslator t : translators){
            sections.add(t.getConfig());
        }

        JsonObject config = new JsonObject();
        config.add("sections", sections);

        //this will get added to our HTML
        String c = config.toString();
    }
}
