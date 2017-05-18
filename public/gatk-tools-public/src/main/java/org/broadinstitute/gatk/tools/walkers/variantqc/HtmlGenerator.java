package org.broadinstitute.gatk.tools.walkers.variantqc;

import com.google.gson.JsonArray;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.gatk.utils.io.Resource;

import javax.swing.text.html.HTML;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Created by bimber on 5/18/2017.
 */
public class HtmlGenerator {
    public static final String[] JS_SCRIPTS = new String[]{
            "assets/js/packages/jquery-3.1.1.min.js",
            "assets/js/packages/jquery-ui.min.js",
            "https://cdnjs.cloudflare.com/ajax/libs/numeral.js/2.0.6/numeral.min.js",
            "assets/js/packages/bootstrap.min.js",
            "assets/js/packages/highcharts.js",
            "assets/js/packages/highcharts.exporting.js",
            "assets/js/packages/highcharts.heatmap.js",
            "assets/js/packages/highcharts.offline-exporting.js",
            "assets/js/packages/jquery.tablesorter.min.js",
            "assets/js/packages/clipboard.min.js",
            "assets/js/packages/FileSaver.min.js",
            "https://cdnjs.cloudflare.com/ajax/libs/chroma-js/1.3.3/chroma.min.js",
            "assets/js/packages/lz-string.min.js",
            "assets/js/summaryTable.js",
            "assets/js/shim.js"
    };

    public static final String[] JS_SCRIPTS2 = new String[]{
            "assets/js/multiqc.js",
            "assets/js/multiqc_tables.js",
            "assets/js/multiqc_toolbox.js",
            "assets/js/multiqc.js",
            "assets/js/multiqc_plotting.js"
    };

    public static final String[] CSS_FILES = new String[]{
            "assets/css/bootstrap.min.css",
            "assets/css/default_multiqc.css",
            "assets/css/font.css"
    };

    public HtmlGenerator() {

    }

    public void generateHtml(List<JsonTranslator> translatorList, PrintStream out) throws IOException {

        //append header
        Resource header = new Resource("templates/template1.html", VariantQC.class);
        IOUtils.copy(header.getResourceContentsAsStream(), out);

        //scripts:
        for (String script : CSS_FILES){
            Resource r = new Resource(script, VariantQC.class);
            out.println("<style>");
            IOUtils.copy(r.getResourceContentsAsStream(), out);
            out.println("</style>");
        }

        for (String script : JS_SCRIPTS){
            Resource r = new Resource(script, VariantQC.class);
            out.println("<script type=\"text/javascript\">");
            IOUtils.copy(r.getResourceContentsAsStream(), out);
            out.println("</script>");
        }

        //config:
        out.println("<script type=\"text/javascript\">");
        out.println("mqc_plots = {};");
        out.println("num_datasets_plot_limit = 50;");
        out.println("$(function() {");
        out.println("processPlots({");
        out.println("sections:");

        JsonArray arr = new JsonArray();
        for (JsonTranslator t : translatorList){
            arr.add(t.getConfig());
        }
        out.println(arr.toString());
        out.println("});");
        out.println("});");
        out.println("</script>");

        for (String script : JS_SCRIPTS2){
            Resource r = new Resource(script, VariantQC.class);
            out.println("<script type=\"text/javascript\">");
            IOUtils.copy(r.getResourceContentsAsStream(), out);
            out.println("</script>");
        }

        //append header
        Resource header2 = new Resource("templates/template2.html", VariantQC.class);
        IOUtils.copy(header.getResourceContentsAsStream(), out);
    }
}
