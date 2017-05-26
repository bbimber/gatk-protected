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
    Gson gson = new GsonBuilder().create();

    public TableReportDescriptor(String label, SectionJsonDescriptor.PlotType plotType, String evaluatorModuleName) {
        super(label, plotType, evaluatorModuleName);
    }

    public static TableReportDescriptor getCountVariantsTable() {
        TableReportDescriptor ret = new TableReportDescriptor("Variant Summary", SectionJsonDescriptor.PlotType.data_table, "CountVariants");

        JsonObject myColJson = new JsonObject();
        myColJson.addProperty("dmin", 0);
        myColJson.addProperty("dmax", 1.0);
        ret.addColumnInfo("myColumn", myColJson);

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
            JsonArray rowJson = new JsonArray();
            List<Object> rowList = new ArrayList<>();
            for (GATKReportColumn col : table.getColumnInfo()) {
                rowList.add(table.get(rowId, col.getColumnName()));
                rowJson = gson.toJsonTree(rowList).getAsJsonArray();
            }
            datasetsJson.add(rowJson);
        }
        dataObj.add("datasets", datasetsJson);

        dataObj.add("columns", new JsonArray());
        for (GATKReportColumn col : table.getColumnInfo()) {
            JsonObject colJson = new JsonObject();
            colJson.addProperty("name", col.getColumnName());
            colJson.addProperty("label", col.getColumnName());

            Object dataType = col.getDataType();
            if (dataType.toString().equals(GATKReportDataType.Decimal.toString())){
                colJson.addProperty("formatString", "0.00");
                List<Double> rowValuesList = new ArrayList<>();
                for (Object rowId : table.getRowIDs()) {
                    rowValuesList.add(NumberUtils.createNumber(table.get(rowId, col.getColumnName()).toString()).doubleValue());
                }
                Double min = Collections.min(rowValuesList) - Collections.min(rowValuesList)*0.1;
                Double max = Collections.max(rowValuesList) + Collections.max(rowValuesList)*0.1;
                colJson.addProperty("dmin", min);
                if (max == 0){
                    colJson.addProperty("dmax", max+1);
                } else {
                    colJson.addProperty("dmax", max);
                }
            } else if (dataType.toString().equals(GATKReportDataType.Integer.toString())){
                colJson.addProperty("formatString", "");
                List<Integer> rowValuesList = new ArrayList<>();
                for (Object rowId : table.getRowIDs()) {
                    rowValuesList.add(NumberUtils.createNumber(table.get(rowId, col.getColumnName()).toString()).intValue());
                }
                Double min = Collections.min(rowValuesList) - Collections.min(rowValuesList)*0.1;
                Double max = Collections.max(rowValuesList) + Collections.max(rowValuesList)*0.1;
                colJson.addProperty("dmin", min);
                if (max == 0){
                    colJson.addProperty("dmax", max+1);
                } else {
                    colJson.addProperty("dmax", max);
                }
            } else {
                colJson.addProperty("formatString", "");
                colJson.addProperty("dmin", "");
                colJson.addProperty("dmax", "");
            }

//            if (columnInfoMap.containsKey(col.getColumnName())){
//                for (Map.Entry<String, JsonElement> e : columnInfoMap.get(col.getColumnName()).entrySet()){
//                    colJson.add(e.getKey(), e.getValue());
//                }
//            }
            dataObj.getAsJsonArray("columns").add(colJson);
        }

        return ret;

    }
}
