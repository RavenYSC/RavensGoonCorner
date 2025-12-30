package com.raven.client.Renderer;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.util.ResourceLocation;

public class MobModelDefinition {
    public final ModelBiped model;
    public final ResourceLocation texture;

    public MobModelDefinition(ModelBiped model, ResourceLocation texture) {
        this.model = model;
        this.texture = texture;
    }
}