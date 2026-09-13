package dev.darkvisuals.client.util.models;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

 
@Environment(EnvType.CLIENT)
public class GibModel {

    private static final Identifier TEXTURE = Identifier.of("darkvisuals", "textures/owl_white.png");

     
    private static final float[] FUR       = {0.95F, 0.95F, 0.93F};  
    private static final float[] FUR_SHADE = {0.86F, 0.86F, 0.84F};  
    private static final float[] EAR_IN    = {0.90F, 0.73F, 0.75F};  
    private static final float[] NOSE      = {0.90F, 0.58F, 0.62F};  
    private static final float[] EYE       = {0.13F, 0.13F, 0.15F};  

    private float headYaw;
    private float headPitch;
    private float bodyYaw;
    private float frontLegX;
    private float backLegX;
    private float bodyRoll;
    private float bobY;
    private float tailSway;

    public void setRotationAngles(float ageInTicks, GibBrain brain) {
        this.headYaw   = -brain.getYaw()   * (float) (Math.PI / 180.0);
        this.headPitch =  brain.getPitch() * (float) (Math.PI / 180.0);
        this.bodyYaw   =  brain.getBody();

        float swing  = brain.limbSwing;
        float amount = brain.limbSwingAmount;

         
        frontLegX = MathHelper.cos(swing * 0.7F)                   * 0.95F * amount;
        backLegX  = MathHelper.cos(swing * 0.7F + (float) Math.PI) * 0.95F * amount;

         
        bodyRoll = MathHelper.cos(swing * 0.7F) * 0.09F * amount;
        bobY = Math.abs(MathHelper.sin(swing * 0.7F)) * 0.05F * amount
                + MathHelper.sin(ageInTicks * 0.08F) * 0.006F;  

         
        tailSway = MathHelper.sin(ageInTicks * 0.12F) * 0.12F
                + MathHelper.cos(swing * 0.7F) * 0.15F * amount;
    }

    public void render(MatrixStack ms, VertexConsumerProvider vcp, GibBrain brain, int light) {
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        int overlay = OverlayTexture.DEFAULT_UV;

        ms.push();
         
        ms.translate(0.0, 0.55F + bobY, 0.0);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bodyYaw));
        ms.multiply(RotationAxis.POSITIVE_Z.rotation(bodyRoll));

         
        float jumpPitch = (float) MathHelper.clamp(brain.getVelocityY() * -25.0, -30.0, 30.0);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(jumpPitch));

        renderBody(ms, vc, light, overlay);
        renderHead(ms, vc, light, overlay);
        renderTail(ms, vc, light, overlay);
        renderLegs(ms, vc, light, overlay);
        ms.pop();
    }

      
    private void renderBody(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();

         
        cube(ms, vc, light, overlay, -2.6F, -3.2F, -2.0F, 5.2F, 4.4F, 4.5F, FUR);

         
        cube(ms, vc, light, overlay, -2.3F, -1.8F, 2.0F, 4.6F, 4.2F, 3.4F, FUR);

         
        cube(ms, vc, light, overlay, -2.2F, -1.6F, -4.6F, 4.4F, 4.8F, 3.0F, FUR);

        ms.pop();
    }

      
    private void renderHead(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0F, 0.03F, -0.30F);  
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(headYaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(headPitch * 0.6F));

         
        cube(ms, vc, light, overlay, -2.6F, -2.2F, -4.6F, 5.2F, 4.6F, 4.8F, FUR);

         
        cube(ms, vc, light, overlay, -2.5F, -3.7F, -1.4F, 1.7F, 1.6F, 2.0F, FUR);
        cube(ms, vc, light, overlay,  0.8F, -3.7F, -1.4F, 1.7F, 1.6F, 2.0F, FUR);
        cube(ms, vc, light, overlay, -2.2F, -3.4F, -1.7F, 1.1F, 1.1F, 0.4F, EAR_IN);
        cube(ms, vc, light, overlay,  1.1F, -3.4F, -1.7F, 1.1F, 1.1F, 0.4F, EAR_IN);

         
        cube(ms, vc, light, overlay, -1.9F, -1.2F, -4.85F, 1.1F, 1.1F, 0.4F, EYE);
        cube(ms, vc, light, overlay,  0.8F, -1.2F, -4.85F, 1.1F, 1.1F, 0.4F, EYE);

         
        cube(ms, vc, light, overlay, -0.5F, 0.3F, -4.95F, 1.0F, 0.8F, 0.5F, NOSE);

         
        cube(ms, vc, light, overlay, -0.9F, 1.1F, -4.8F, 1.8F, 0.9F, 0.4F, FUR_SHADE);

        ms.pop();
    }

      
    private void renderTail(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0F, -0.06F, 0.32F);  
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(tailSway));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(14.0F));  

         
        cube(ms, vc, light, overlay, -0.9F, -0.9F, 0.0F, 1.8F, 1.8F, 4.8F, FUR);
         
        cube(ms, vc, light, overlay, -1.3F, -1.3F, 4.3F, 2.6F, 2.6F, 2.9F, FUR);

        ms.pop();
    }

      
    private void renderLegs(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
         
        ms.push();
        ms.translate(0.03F, 0.175F, -0.20F);
        ms.multiply(RotationAxis.POSITIVE_X.rotation(frontLegX));
        cube(ms, vc, light, overlay, -1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, FUR);
         
        cube(ms, vc, light, overlay, -1.1F, 4.6F, -2.4F, 2.2F, 1.4F, 3.4F, FUR);
        ms.pop();

         
        ms.push();
        ms.translate(-0.03F, 0.125F, 0.225F);
        ms.multiply(RotationAxis.POSITIVE_X.rotation(backLegX));
         
        cube(ms, vc, light, overlay, -1.2F, 0.0F, -1.3F, 2.4F, 3.4F, 2.6F, FUR);
         
        cube(ms, vc, light, overlay, -0.9F, 3.2F, -0.9F, 1.8F, 2.4F, 1.8F, FUR);
         
        cube(ms, vc, light, overlay, -1.0F, 5.4F, -2.3F, 2.0F, 1.4F, 3.2F, FUR);
        ms.pop();
    }

    private void cube(MatrixStack ms, VertexConsumer vc, int light, int overlay,
                      float ox, float oy, float oz, float w, float h, float d,
                      float[] color) {
        float x0 = ox * 0.0625F;
        float x1 = (ox + w) * 0.0625F;
        float y0 = oy * 0.0625F;
        float y1 = (oy + h) * 0.0625F;
        float z0 = oz * 0.0625F;
        float z1 = (oz + d) * 0.0625F;

        Matrix4f m4 = ms.peek().getPositionMatrix();
        Entry entry = ms.peek();
        float r = color[0], g = color[1], b = color[2];

        quad(vc, m4, entry, light, overlay, r, g, b, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, 1,0,0);
        quad(vc, m4, entry, light, overlay, r, g, b, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1,0,0);
        quad(vc, m4, entry, light, overlay, r, g, b, x1,y0,z1, x0,y0,z1, x0,y0,z0, x1,y0,z0, 0,-1,0);
        quad(vc, m4, entry, light, overlay, r, g, b, x1,y1,z0, x0,y1,z0, x0,y1,z1, x1,y1,z1, 0,1,0);
        quad(vc, m4, entry, light, overlay, r, g, b, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, 0,0,-1);
        quad(vc, m4, entry, light, overlay, r, g, b, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, 0,0,1);
    }

    private void quad(VertexConsumer vc, Matrix4f m4, Entry entry, int light, int overlay,
                      float r, float g, float b,
                      float x0, float y0, float z0,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float nx, float ny, float nz) {
        vc.vertex(m4, x0, y0, z0).texture(0.25F, 0.25F).color(r, g, b, 1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x1, y1, z1).texture(0.75F, 0.25F).color(r, g, b, 1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x2, y2, z2).texture(0.75F, 0.75F).color(r, g, b, 1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x3, y3, z3).texture(0.25F, 0.75F).color(r, g, b, 1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
    }
}
