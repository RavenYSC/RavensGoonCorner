package com.raven.client.Renderer;

import com.raven.client.models.BatTexture;
import com.raven.client.models.DragonTexture;
import com.raven.client.models.EndermanTexture;
import com.raven.client.models.IronGolemTexture;
import com.raven.client.models.SkeletonTexture;
import com.raven.client.models.SpiderTexture;
import com.raven.client.models.WitherTexture;
import com.raven.client.models.ZombieTexture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityBat;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

import java.util.HashMap;
import java.util.Map;

public class ClientHandler {

    @SuppressWarnings("deprecation")
	public static void registerRenderers() {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();

        Map<Class<? extends EntityLiving>, MobModelDefinition> mobMap = new HashMap<>();

        mobMap.put(EntityZombie.class, new MobModelDefinition(
        		new ZombieTexture(), new net.minecraft.util.ResourceLocation("ravenclient", "textures/entity/ZombieCustom.png")
           	));

        mobMap.put(EntitySkeleton.class, new MobModelDefinition(
        		new SkeletonTexture(), new net.minecraft.util.ResourceLocation("ravenclient", "textures/entity/SkeletonCustom.png")
        	));
        
        mobMap.put(EntitySpider.class, new MobModelDefinition(
                new SpiderTexture(), new net.minecraft.util.ResourceLocation("ravenclient", "textures/entity/SpiderCustom.png")
            ));
        
        mobMap.put(EntityWither.class, new MobModelDefinition(
                new WitherTexture(), new net.minecraft.util.ResourceLocation("ravenclient", "textures/entity/WitherCustom.png")
            ));
        
        mobMap.put(EntityDragon.class, new MobModelDefinition(
                new DragonTexture(), new net.minecraft.util.ResourceLocation("ravenclient", "textures/entity/DragonCustom.png")
            ));
        
        mobMap.put(EntityGolem.class, new MobModelDefinition(
                new IronGolemTexture(), new net.minecraft.util.ResourceLocation("ravenclient", "textures/entity/IronGolemCustom.png")
            ));
        
        mobMap.put(EntityBat.class, new MobModelDefinition(
                new BatTexture(), new net.minecraft.util.ResourceLocation("ravenclient", "textures/entity/BatCustom.png")
            ));
        
        mobMap.put(EntityEnderman.class, new MobModelDefinition(
                new EndermanTexture(), new net.minecraft.util.ResourceLocation("ravenclient", "textures/entity/EndermanCustom.png")
            ));
        
        for (Map.Entry<Class<? extends EntityLiving>, MobModelDefinition> entry : mobMap.entrySet()) {
            Class<? extends EntityLiving> entityClass = entry.getKey();

            RenderingRegistry.registerEntityRenderingHandler(entityClass,
                new ModularMobRenderer<>(manager, entry.getValue().model, entry.getValue().texture));
        }
    }
}