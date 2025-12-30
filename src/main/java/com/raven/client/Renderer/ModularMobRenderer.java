package com.raven.client.Renderer;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

public class ModularMobRenderer<T extends EntityLiving> extends RenderLiving<T> {

    private final ResourceLocation texture;

    public ModularMobRenderer(RenderManager renderManager, ModelBiped model, ResourceLocation texture) {
        super(renderManager, model, 0.5F); // shadow size = 0.5F
        this.texture = texture;
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return texture;
    }
}
