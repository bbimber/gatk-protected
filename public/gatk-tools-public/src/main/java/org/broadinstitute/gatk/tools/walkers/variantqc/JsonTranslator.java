package org.broadinstitute.gatk.tools.walkers.variantqc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang.math.NumberUtils;
import org.broadinstitute.gatk.utils.report.GATKReportColumn;
import org.broadinstitute.gatk.utils.report.GATKReportTable;

import java.util.*;

/**
 * Created by bimber on 5/12/2017.
 */
public class JsonTranslator {
    protected final GATKReportTable _table;
    protected final String _sectionLabel;
    protected final List<ReportDescriptor> _rds;
    protected final String[] _stratifications;

    protected enum PlotType {
        data_table(),
        bar_graph(),
        xy_line();
    };

    public JsonTranslator(String sectionLabel, GATKReportTable table, String[] stratifications, List<ReportDescriptor> reportDescriptors){
        _sectionLabel = sectionLabel;
        _table = table;
        _rds = reportDescriptors;
        _stratifications = stratifications;
    }

    public JsonObject getConfig(){
        JsonObject ret = new JsonObject();
        ret.addProperty("label", _sectionLabel);

        JsonArray reports = new JsonArray();
        for (ReportDescriptor rd : _rds){
            reports.add(rd.getReportJson(_sectionLabel, _table));
        }
        ret.add("reports", reports);
        
        return ret;
    }

    abstract static class ReportDescriptor {
        protected final String label;
        protected JsonTranslator.PlotType plotType;
        protected String evaluatorModuleName;
        protected Map<String, JsonObject> columnInfoMap;

        protected ReportDescriptor(String label, JsonTranslator.PlotType plotType, String evaluatorModuleName){
            this.label = label;
            this.plotType = plotType;
            this.evaluatorModuleName = evaluatorModuleName;
            this.columnInfoMap = new HashMap<>();
        }

        protected int getColumnByName(GATKReportTable table, String name){
            int idx = 0;
            for (GATKReportColumn col : table.getColumnInfo()){
                if (col.getColumnName().equals(name)){
                    return idx;
                }

                idx++;
            }

            return -1;
        }

        abstract JsonObject getReportJson(String sectionTitle, GATKReportTable table);

        protected JsonArray inferSampleNames(GATKReportTable table){
            JsonArray ret = new JsonArray();
            for (Object id : table.getRowIDs()){
                ret.add(new JsonPrimitive(id.toString()));
            }

            return ret;
        }
    }

    public static class TableReportDescriptor extends ReportDescriptor {
        public TableReportDescriptor(String label, JsonTranslator.PlotType plotType, String evaluatorModuleName){
            super(label, plotType, evaluatorModuleName);
        }

        public static TableReportDescriptor getCountVariantsTable(){
            return new TableReportDescriptor("Variant Summary", JsonTranslator.PlotType.data_table, "CountVariants");
        }
        
        @Override
        public JsonObject getReportJson(String sectionTitle, GATKReportTable table){
            JsonObject ret = new JsonObject();
            ret.addProperty("label", label);

            JsonObject data = new JsonObject();
            ret.add("data", data);

            data.addProperty("plot_type", plotType.name());
            data.add("samples", inferSampleNames(table));
            data.add("datasets", new JsonArray());

            data.add("columns", new JsonArray());
            for (GATKReportColumn col : table.getColumnInfo()){
                JsonObject colJson = new JsonObject();
                colJson.addProperty("name", col.getColumnName());
                colJson.addProperty("label", col.getColumnName());

                //we will probably need a way to provide information beyond just what is in the GATKReportTable itself
                if (columnInfoMap.containsKey(col.getColumnName())){
                    for (Map.Entry<String, JsonElement> entry : columnInfoMap.get(col.getColumnName()).entrySet()){
                        colJson.add(entry.getKey(), entry.getValue());
                    }
                }

                //colJson.addProperty("formatString", "");
                if (!colJson.has("dmin")){
                    //TODO: infer based on data.  perhaps find the min/max and take +/- 10%?
                }

                data.getAsJsonArray("columns").add(colJson);
            }

            return ret;
        }
    }
    
    public static class BarPlotReportDescriptor extends ReportDescriptor {
        private String[] columnsToPlot;
        private String yLabel;

        public BarPlotReportDescriptor(String plotTitle, JsonTranslator.PlotType plotType, String evaluatorModuleName, String[] columnsToPlot, String yLabel){
            super(plotTitle, plotType, evaluatorModuleName);
            this.columnsToPlot = columnsToPlot;
            this.yLabel = yLabel;
        }

        public static BarPlotReportDescriptor getBarPlot(String evalModule, String plotTitle, String[] colNames, String yLabel) {
            return new BarPlotReportDescriptor(plotTitle, JsonTranslator.PlotType.bar_graph, evalModule, colNames, yLabel);
        }

        public static BarPlotReportDescriptor getVariantTypeBarPlot(){
            return new BarPlotReportDescriptor( "Variant Type", JsonTranslator.PlotType.bar_graph, "CountVariants", new String[]{"nSNPs", "nMNPs", "nInsertions", "nDeletions", "nComplex", "nSymbolic", "nMixed"}, "# Variants");
        }

        @Override
        public JsonObject getReportJson(String sectionTitle, GATKReportTable table){
            JsonObject ret = new JsonObject();
            ret.addProperty("label", label);

            JsonObject dataObj = new JsonObject();
            ret.add("data", dataObj);

            dataObj.addProperty("plot_type", plotType.name());

            JsonArray samples = inferSampleNames(table);
            dataObj.add("samples", new JsonArray());
            dataObj.getAsJsonArray("samples").add(samples);

            JsonArray datasetsJson = new JsonArray();
            for (String colName : columnsToPlot){
                int colIdx = getColumnByName(table, colName);

                JsonObject datasetJson = new JsonObject();
                //datasetJson.addProperty("color", "");
                datasetJson.addProperty("name", colName);

                JsonArray data = new JsonArray();
                for (int i=0;i<table.getNumRows();i++){
                    data.add(new JsonPrimitive(NumberUtils.createNumber(table.get(i, colIdx).toString())));
                }
                datasetJson.add("data", data);

                datasetsJson.add(datasetJson);
            }
            dataObj.add("datasets", new JsonArray());
            dataObj.getAsJsonArray("datasets").add(datasetsJson);

            JsonObject configJson = new JsonObject();
            configJson.addProperty("ylab", this.yLabel);
            configJson.addProperty("title", label);

            dataObj.add("config", configJson);

            return ret;
        }
    }

//    public static class XYLineReportDescriptor extends ReportDescriptor
//    {
//        private String[] columnsToPlot;
//        private String yLabel;
//
//        public XYLineReportDescriptor(String plotTitle, JsonTranslator.PlotType plotType, String evaluatorModuleName){
//            super(plotTitle, plotType, evaluatorModuleName);
//        }
//
//        @Override
//        void JsonObject getReportJson(String sectionTitle, GATKReportTable table) {
//            JsonArray samples = inferSampleNames(table);
//            ret.add("samples", samples);
//
//            JsonArray datasetsJson = new JsonArray();
//            JsonObject colorJson = new JsonObject();
//            for (String colName : columnsToPlot){
//                int colIdx = getColumnByName(table, colName);
//
//                JsonObject datasetJson = new JsonObject();
//                //datasetJson.addProperty("color", "");
//                datasetJson.addProperty("name", colName);
//
//                JsonArray data = new JsonArray();
//                for (int i=0;i<table.getNumRows();i++){
//                    data.add(new JsonPrimitive(NumberUtils.createNumber(table.get(i, colIdx).toString())));
//                }
//                datasetJson.add("data", data);
//            }
//
//
//
//            //colorJson.addProperty(sample, null);
//
//            //"data": [[1, 32.385829384477994], [2, 32.51257319784182], [3, 32.53051509589336], [4, 35.930739475597946], [5, 35.91741590046593], [6, 35.928392537730666], [7, 35.91026610496744], [8, 35.897712428885164], [9, 37.57662158757583], [10, 37.585727700319836], [12, 37.53505206612946], [14, 38.92510002043037], [16, 38.86395021022102], [18, 38.8059641509619], [20, 38.73872162799532], [22, 38.63794941362771], [24, 38.520917890439165], [26, 38.36361591192953], [28, 38.230401956153074], [30, 38.06566984593516], [32, 37.91252182451145], [34, 37.841280868131435], [36, 37.878115239254726], [38, 37.8159351759587], [40, 37.661726689574806], [42, 37.51950302387578], [44, 37.33677994331231], [46, 37.13236408525927], [48, 36.92583309222985], [50, 36.258499335319016], [52, 36.14480792257834], [54, 36.53248978790511], [56, 36.499392821520985], [58, 36.315043984495425], [60, 36.074915082678686], [62, 35.79003837432995], [64, 35.528671738335916], [66, 35.24913007505768], [68, 34.97733730062913], [70, 34.72833874478797], [72, 34.4612219301588], [74, 34.21380766485854], [76, 33.99092253379628], [78, 33.77986131866412], [80, 33.591852978159906], [82, 33.43427846620381], [84, 33.291657585804636], [86, 33.19623853098773], [88, 33.10817420745926], [90, 33.005813719535894], [92, 32.89738799884225], [94, 32.93315393977193], [96, 33.019364145676036], [98, 33.18226949894594], [100, 32.70148968981316]],
//
//            JsonObject configJson = new JsonObject();
//            configJson.addProperty("xlab", this.yLabel);
//
//            configJson.add("colors", colorJson);
//            configJson.addProperty("tt_label", "<b>Base {point.x}</b>: {point.y:.2f}");
//            configJson.addProperty("xDecimals", false);
//            configJson.addProperty("title", label);
//            configJson.addProperty("ylab", this.yLabel);
//
//            ret.add("config", configJson);
//        }
//    }
}
