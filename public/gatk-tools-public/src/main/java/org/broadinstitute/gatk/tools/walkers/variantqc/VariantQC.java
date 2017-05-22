package org.broadinstitute.gatk.tools.walkers.variantqc;

import htsjdk.variant.variantcontext.VariantContext;
import org.broadinstitute.gatk.engine.arguments.DbsnpArgumentCollection;
import org.broadinstitute.gatk.engine.arguments.StandardVariantContextInputArgumentCollection;
import org.broadinstitute.gatk.engine.walkers.*;
import org.broadinstitute.gatk.tools.walkers.varianteval.VariantEval;
import org.broadinstitute.gatk.utils.classloader.JVMUtils;
import org.broadinstitute.gatk.utils.commandline.ArgumentCollection;
import org.broadinstitute.gatk.utils.commandline.Output;
import org.broadinstitute.gatk.utils.commandline.RodBinding;
import org.broadinstitute.gatk.utils.commandline.Tags;
import org.broadinstitute.gatk.utils.contexts.AlignmentContext;
import org.broadinstitute.gatk.utils.contexts.ReferenceContext;
import org.broadinstitute.gatk.utils.exceptions.GATKException;
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
        sampleStratifiedWalker.setToolkit(getToolkit());
        locusStratifiedWalker = new VariantEval();
        locusStratifiedWalker.setToolkit(getToolkit());

        //manually set arguments
        try
        {
            Field evalField = VariantEval.class.getDeclaredField("evals");
            JVMUtils.setFieldValue(evalField, sampleStratifiedWalker, Arrays.asList(variantCollection.variants));
            JVMUtils.setFieldValue(evalField, locusStratifiedWalker, Arrays.asList(variantCollection.variants));

            Field outField = VariantEval.class.getDeclaredField("out");
            JVMUtils.setFieldValue(outField, sampleStratifiedWalker, new PrintStream(bao1));
            JVMUtils.setFieldValue(outField, locusStratifiedWalker, new PrintStream(bao2));

            Field stratificationsToUseField = VariantEval.class.getDeclaredField("MODULES_TO_USE");
            JVMUtils.setFieldValue(stratificationsToUseField, sampleStratifiedWalker, new String[]{"CountVariants", "IndelSummary", "TiTvVariantEvaluator", "GenotypeFilterSummary"});
            JVMUtils.setFieldValue(stratificationsToUseField, locusStratifiedWalker, new String[]{"CountVariants", "IndelSummary", "TiTvVariantEvaluator", "GenotypeFilterSummary"});

            Field noevField = VariantEval.class.getDeclaredField("NO_STANDARD_MODULES");
            JVMUtils.setFieldValue(noevField, sampleStratifiedWalker, true);
            JVMUtils.setFieldValue(noevField, locusStratifiedWalker, true);

            Field modulesField = VariantEval.class.getDeclaredField("STRATIFICATIONS_TO_USE");
            JVMUtils.setFieldValue(modulesField, sampleStratifiedWalker, new String[]{"Sample"});
            JVMUtils.setFieldValue(modulesField, locusStratifiedWalker, new String[]{"Contig"});

            //TODO: set unbound?
            Field dbSnpField= VariantEval.class.getDeclaredField("dbsnp");
            dbSnpField.setAccessible(true);

            DbsnpArgumentCollection dbsnpArgumentCollection1 = (DbsnpArgumentCollection)dbSnpField.get(sampleStratifiedWalker);
            dbsnpArgumentCollection1.dbsnp = new RodBinding<>(VariantContext.class, "", "UNBOUND", "", new Tags());
            JVMUtils.setFieldValue(dbSnpField, sampleStratifiedWalker, dbsnpArgumentCollection1);

            DbsnpArgumentCollection dbsnpArgumentCollection2 = (DbsnpArgumentCollection)dbSnpField.get(locusStratifiedWalker);
            dbsnpArgumentCollection2.dbsnp = new RodBinding<>(VariantContext.class, "", "UNBOUND", "", new Tags());
            JVMUtils.setFieldValue(dbSnpField, locusStratifiedWalker, dbsnpArgumentCollection2);

            //TODO: Stratification / VariantEvaluator combinations we likely need:

            //Entire genome (EvalRod):
            //CountVariants, IndelSummary, TiTvVariantEvaluator, GenotypeFilterSummary, MendelianViolationEvaluator
            //SiteFilterSummary? VariantSummary?

            //Locus: probably identical to EvalRod

            //Sample:
            //CountVariants, IndelSummary, TiTvVariantEvaluator, GenotypeFilterSummary, MendelianViolationEvaluator
            //SiteFilterSummary?
            //NOTE: VariantSummary is incompatible w/ Sample.

            //FilterType:
            //NOTE: we might need to implement the per-filter type binning as a stratification, which is non-ideal
            //This is only needed if we cant get SiteFilterSummary working
            //CountVariants, IndelSummary, GenotypeFilterSummary

            //Sample + FilterType:
            //Same as FilterType
        }
        catch (Exception e)
        {
            throw new GATKException(e.getMessage(), e);
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
        //sampleStratifiedWalker.reduceInit();
        //locusStratifiedWalker.reduceInit();

        return null;
    }

    @Override
    public Integer reduce(Integer value, Integer sum) {
        //sampleStratifiedWalker.reduce(value, sum);
        //locusStratifiedWalker.reduce(value, sum);

        return null;
    }

    @Override
    public void onTraversalDone(Integer result) {
        super.onTraversalDone(result);

        sampleStratifiedWalker.onTraversalDone(result);
        locusStratifiedWalker.onTraversalDone(result);

        //make classes like this to translate from the GATKReportTable into the config object we need in our HTML
        List<JsonTranslator> translators = new ArrayList<>();

        try (BufferedReader sampleReader = new BufferedReader(new StringReader(new String(bao1.toByteArray())))) {
            sampleReader.readLine(); //read first GATKReport line

            //this output will likely contain multiple reports, and we can add them like this.
            //the reader will only scan to the end of its table, and then leave the reader on the beginning of the next table
            translators.add(new JsonTranslator(new GATKReportTable(sampleReader, GATKReportVersion.V1_1), "Plot1", JsonTranslator.PlotType.data_table, "VariantSummary", Arrays.asList("Sample")));
            translators.add(new JsonTranslator(new GATKReportTable(sampleReader, GATKReportVersion.V1_1), "Plot2", JsonTranslator.PlotType.data_table, "IndelSummary", Arrays.asList("Sample")));
        }
        catch (IOException e) {
            throw new GATKException(e.getMessage(), e);
        }

        try {
            HtmlGenerator generator = new HtmlGenerator();
            generator.generateHtml(translators, out);
        }
        catch (IOException e){
            throw new GATKException(e.getMessage(), e);
        }
    }
}
