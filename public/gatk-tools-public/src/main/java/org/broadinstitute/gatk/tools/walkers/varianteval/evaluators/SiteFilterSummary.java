package org.broadinstitute.gatk.tools.walkers.varianteval.evaluators;

import htsjdk.variant.variantcontext.VariantContext;
import org.broadinstitute.gatk.tools.walkers.varianteval.util.DataPoint;
import org.broadinstitute.gatk.utils.contexts.AlignmentContext;
import org.broadinstitute.gatk.utils.contexts.ReferenceContext;
import org.broadinstitute.gatk.utils.refdata.RefMetaDataTracker;

/**
 * Created by bimber on 5/17/2017.
 */
public class SiteFilterSummary extends VariantEvaluator {
    @DataPoint(description = "Number of SNPs", format = "%d")
    public long nSites = 0;

    @Override
    public int getComparisonOrder() {
        return 1;
    }

    @Override
    public void update1(VariantContext eval, RefMetaDataTracker tracker, ReferenceContext ref, AlignmentContext context) {
        nSites += 1;
    }
}
