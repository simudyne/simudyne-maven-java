package org.example.models.forestFire;

import simudyne.core.schema.FieldType;
import simudyne.core.schema.SchemaEnum;
import simudyne.core.schema.SchemaField;
import simudyne.core.schema.SchemaRecord;
import simudyne.core.values.ValueRecord;

import java.util.Arrays;

public class Monitor {
    public long agentID;
    public State state;

    public Monitor(long agentID, State state) {
        this.agentID = agentID;
        this.state = state;
    }

    public static SchemaRecord getMonitorSchema() {
        return new SchemaRecord("monitor")
                .add(new SchemaField("agentID", FieldType.Long))
                .add(new SchemaEnum("agent_status", Arrays.asList("EMPTY",
                        "HEALTHY",
                        "INFECTED",
                        "RESISTANT")));
    }

    public ValueRecord getMonitorValue() {
        return new ValueRecord("monitor")
                .addField("agentID", this.agentID)
               .addField("agent_status", this.state);
    }

}
