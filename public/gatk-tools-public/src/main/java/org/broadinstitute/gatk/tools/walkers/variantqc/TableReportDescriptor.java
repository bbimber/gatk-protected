package org.broadinstitute.gatk.tools.walkers.variantqc;

import com.google.gson.*;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.broadinstitute.gatk.utils.report.GATKReportColumn;
import org.broadinstitute.gatk.utils.report.GATKReportDataType;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Created by bimber on 5/22/2017.
 */
public class TableReportDescriptor extends ReportDescriptor {
    private Gson gson = new GsonBuilder().create();
    private Set<String> skippedColNames = new HashSet<>();

    public TableReportDescriptor(String label, String evaluatorModuleName) {
        super(label, SectionJsonDescriptor.PlotType.data_table, evaluatorModuleName);
        skippedColNames.add(evaluatorModuleName);
        skippedColNames.add("EvalRod");
        skippedColNames.add("CompRod");
    }

    public static TableReportDescriptor getCountVariantsTable() {
        TableReportDescriptor ret = new TableReportDescriptor("Variant Summary", "CountVariants");

        //JsonObject myColJson = new JsonObject();
        //myColJson.addProperty("dmin", 0);
        //myColJson.addProperty("dmax", 1.0);
        //ret.addColumnInfo("myColumn", myColJson);

        return ret;
    }

    @Override
    public JsonObject getReportJson(String sectionTitle) {
        JsonObject ret = new JsonObject();
        ret.addProperty("label", label);

        JsonObject dataObj = new JsonObject();
        ret.add("data", dataObj);

        dataObj.addProperty("plot_type", plotType.name());

        dataObj.add("samples", getSampleNames());//Ordering of sample names must correspond with dataset order

        JsonArray datasetsJson = new JsonArray();
        for (Object rowId : table.getRowIDs()) {
            List<Object> rowList = new ArrayList<>();
            for (GATKReportColumn col : table.getColumnInfo()) {
                if (skippedColNames.contains(col.getColumnName())){
                    continue;
                }

                rowList.add(table.get(rowId, col.getColumnName()));
            }

            datasetsJson.add(gson.toJsonTree(rowList).getAsJsonArray());
        }
        dataObj.add("datasets", datasetsJson);

        dataObj.add("columns", new JsonArray());
        for (GATKReportColumn col : table.getColumnInfo()) {
            if (skippedColNames.contains(col.getColumnName())){
                continue;
            }

            JsonObject colJson = new JsonObject();
            colJson.addProperty("name", col.getColumnName());
            colJson.addProperty("label", col.getColumnName());

            if (col.getDataType() == GATKReportDataType.Decimal){
                //TODO: look into format strings supporting more than 6 decimals
                //colJson.addProperty("formatString", "0.00");

                inferMinMax(colJson, col.getColumnName());

            } else if (col.getDataType() == GATKReportDataType.Integer){
                inferMinMax(colJson, col.getColumnName());
            }

            //allow upstream code to supply custom config
            if (columnInfoMap.containsKey(col.getColumnName())){
                for (Map.Entry<String, JsonElement> e : columnInfoMap.get(col.getColumnName()).entrySet()){
                    colJson.add(e.getKey(), e.getValue());
                }
            }

            dataObj.getAsJsonArray("columns").add(colJson);
        }

        return ret;

    }

    private void inferMinMax(JsonObject colJson, String colName){
        List<Double> rowValuesList = new ArrayList<>();
        for (Object rowId : table.getRowIDs()) {
            rowValuesList.add(NumberUtils.createNumber(table.get(rowId, colName).toString()).doubleValue());
        }

        Double min = Collections.min(rowValuesList) - Collections.min(rowValuesList)*0.1;
        Double max = Collections.max(rowValuesList) + Collections.max(rowValuesList)*0.1;
        colJson.addProperty("dmin", min);
        colJson.addProperty("dmax", max == 0 ? 1 : max);
    }
}
