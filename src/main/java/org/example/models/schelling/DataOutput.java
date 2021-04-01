package org.example.models.schelling;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class DataOutput {
    public JSONObject gridHistory;

    public DataOutput() {

        gridHistory = new JSONObject();
    }

    public void addDataValue(long timeStep, DataValue dataValue) {
        try {
            gridHistory.put(String.valueOf(timeStep), dataValue.gridValues);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}