package org.example.models;

import simudyne.core.Model;
import simudyne.core.ModelContext;
import simudyne.core.ModelExecutionPhase;
import simudyne.core.ModelSchemaUtil;
import simudyne.core.channel.OutputChannelWriter;

public abstract class AbstractModel implements Model {
  private OutputChannelWriter outputChannelWriter = null;

  @Override
  public void emitData(ModelContext dataContext, ModelExecutionPhase phase) {
    ModelSchemaUtil.emitData(this, dataContext, phase, outputChannelWriter);
  }

  @Override
  public void registerSchemas(ModelContext dataContext) {
    outputChannelWriter = ModelSchemaUtil.registerSchemas(this, dataContext);
  }
}
