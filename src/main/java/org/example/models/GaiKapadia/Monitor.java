package org.example.models.GaiKapadia;

import simudyne.core.schema.FieldType;
import simudyne.core.schema.SchemaField;
import simudyne.core.schema.SchemaRecord;
import simudyne.core.values.ValueRecord;

public class Monitor {
    public long agentID;
    public long state;
    public double nodeDegree;

    public Monitor(long agentID, long state,double nodeDegree) { //
        this.agentID = agentID;
        this.state = state;
        this.nodeDegree=nodeDegree;
    }

    public static SchemaRecord getMonitorSchema() {
        return new SchemaRecord("monitor")
                .add(new SchemaField("agentID", FieldType.Long))
                .add(new SchemaField("agent_status", FieldType.Long))
                .add(new SchemaField("nodeDegree", FieldType.Double));
    }

    public ValueRecord getMonitorValue() {
        return new ValueRecord("monitor")
                .addField("agentID", this.agentID)
                .addField("agent_status", this.state)
                .addField("nodeDegree", this.nodeDegree);
    }

}
