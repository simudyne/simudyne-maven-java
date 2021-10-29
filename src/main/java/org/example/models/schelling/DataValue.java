package org.example.models.schelling;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataValue {
    public JSONObject gridValues;

    public DataValue(Cell[][] cells) {
        gridValues = new JSONObject();
        for (int i = 0; i < cells.length; i++) {
            Map innerGrid = new LinkedHashMap(cells.length);
            for (int j = 0; j < cells.length; j++) {
                innerGrid.put(j, String.valueOf(convertEnum(cells[i][j].state)));
            }
            try {
                gridValues.put(String.valueOf(i), innerGrid);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public int convertEnum(CellState state) {
        if (state == CellState.BLUE)
            return -1;
        else if (state == CellState.RED)
            return 1;
        else
            return 0;
    }
}
