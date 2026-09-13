package dev.darkvisuals.mixin;

import dev.darkvisuals.emotes.EmoteManager;
import dev.darkvisuals.emotes.EmoteRenderState;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerEntityModel.class)
public abstract class PlayerEmoteModelMixin extends BipedEntityModel<PlayerEntityRenderState> {

    @Shadow @Final public ModelPart leftSleeve;
    @Shadow @Final public ModelPart rightSleeve;
    @Shadow @Final public ModelPart leftPants;
    @Shadow @Final public ModelPart rightPants;
    @Shadow @Final public ModelPart jacket;

    protected PlayerEmoteModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V",
            at = @At("TAIL"))
    private void darkvisuals$applyEmote(PlayerEntityRenderState state, CallbackInfo ci) {
        if (!(state instanceof EmoteRenderState emoteState)) return;
        UUID owner = emoteState.darkvisuals$getEmoteOwner();
        if (owner == null) return;

        EmoteManager.ActiveEmote active = EmoteManager.getActive(owner);
        if (active == null) return;

        active.emote().pose().apply((PlayerEntityModel) (Object) this, active.blend(), active.seconds());

         
         
        this.rightSleeve.copyTransform(this.rightArm);
        this.leftSleeve.copyTransform(this.leftArm);
        this.rightPants.copyTransform(this.rightLeg);
        this.leftPants.copyTransform(this.leftLeg);
        this.jacket.copyTransform(this.body);
        this.hat.copyTransform(this.head);
    }
}