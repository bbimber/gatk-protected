package org.broadinstitute.gatk.tools.walkers.variantqc;

import com.google.gson.JsonObject;
import org.broadinstitute.gatk.utils.report.GATKReportTable;

/**
 * Created by bimber on 5/12/2017.
 */
public class JsonTranslator {
    protected final GATKReportTable _table;
    protected final String _label;
    private PlotType _plotType;

    protected enum PlotType {
        data_table(),
        bar_graph(),
        xy_line();
    };

    public JsonTranslator(GATKReportTable table, String label, PlotType plotType){
        _table = table;
        _label = label;
        _plotType = plotType;
    }

    public JsonObject getConfig(){
        JsonObject ret = new JsonObject();
        ret.addProperty("label", _label);

        ret.add("data", new JsonObject());
        ret.getAsJsonObject("data").addProperty("plot_type", _plotType.name());

        switch (_plotType){
            case bar_graph:
                configureBarGraph(ret);
                break;
            case xy_line:
                configureXYGraph(ret);
                break;
            case data_table:
                configureTable(ret);
                break;
        }
        return ret;
    }

    private void configureBarGraph(JsonObject ret){
        //TODO
    }

    private void configureXYGraph(JsonObject ret){
        //TODO
    }

    private void configureTable(JsonObject ret){
        //TODO
    }
}
