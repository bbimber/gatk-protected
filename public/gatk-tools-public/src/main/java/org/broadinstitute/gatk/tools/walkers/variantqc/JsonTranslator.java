package org.broadinstitute.gatk.tools.walkers.variantqc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.broadinstitute.gatk.utils.report.GATKReportColumn;
import org.broadinstitute.gatk.utils.report.GATKReportTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bimber on 5/12/2017.
 */
public class JsonTranslator {
    protected final GATKReportTable _table;
    protected final String _label;
    private PlotType _plotType;
    private String _evaluatorModuleName;
    private List<String> _stratifications;

    private Map<String, JsonObject> _columnInfoMap;

    protected enum PlotType {
        data_table(),
        bar_graph(),
        xy_line();
    };

    public JsonTranslator(GATKReportTable table, String label, PlotType plotType, String evaluatorModuleName, List<String> stratifications){
        _table = table;
        _label = label;
        _plotType = plotType;
        _evaluatorModuleName = evaluatorModuleName;
        _stratifications = stratifications;
        _columnInfoMap = new HashMap<>();
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
        ret.addProperty("label", _label);
        JsonObject data = new JsonObject();
        data.addProperty("label", _label);
        data.addProperty("plot_type", "data_table");

        data.add("samples", new JsonArray());
        //TODO: need to either pass in upstream or infer from the rows

        data.add("datasets", new JsonArray());
        //build JsonArray of JsonArrays with data per sample (i.e. rows)

        data.add("columns", new JsonArray());
        for (GATKReportColumn col : _table.getColumnInfo()){
            JsonObject colJson = new JsonObject();
            colJson.addProperty("name", col.getColumnName());
            colJson.addProperty("label", col.getColumnName());

            //we will probably need a way to provide information beyond just what is in the GATKReportTable itself
            if (_columnInfoMap.containsKey(col.getColumnName())){
                for (Map.Entry<String, JsonElement> entry : _columnInfoMap.get(col.getColumnName()).entrySet()){
                    colJson.add(entry.getKey(), entry.getValue());
                }
            }

            //colJson.addProperty("formatString", "");
            if (!colJson.has("dmin")){
                //TODO: infer based on data.  perhaps find the min/max and take +/- 10%?
            }

            data.getAsJsonArray("columns").add(colJson);
        }

        ret.add("data", data);


    }
}
