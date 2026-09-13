package dev.darkvisuals.client.util.Network;

import dev.darkvisuals.client.util.render.Wrapper;
import lombok.experimental.UtilityClass;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;

import static dev.darkvisuals.client.util.Wrapper.mc;

@UtilityClass
public class Server implements Wrapper {

    public boolean is(String server) {
        if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null) return false;
        return mc.getNetworkHandler().getServerInfo().address.toLowerCase().contains(server);
    }

    public int getPing(PlayerEntity entity) {
        PlayerListEntry list = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        return list != null ? list.getLatency() : 0;
    }

    public float getHealth(LivingEntity entity, boolean gapple) {
        if (entity == null) {
            return 0f;  
        }

         
        float fallbackHealth = entity.getHealth() + (gapple ? entity.getAbsorptionAmount() : 0f);
        {

            if (entity instanceof PlayerEntity player) {
                 
                ScoreboardObjective objective = player.getScoreboard() != null
                        ? player.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME)
                        : null;
                if (objective == null) {
                    return fallbackHealth;  
                }

                 
                ReadableScoreboardScore score = player.getScoreboard().getScore(player, objective);
                if (score == null) {
                    return fallbackHealth;  
                }

                MutableText text = ReadableScoreboardScore.getFormattedScore(score, objective.getNumberFormatOr(StyledNumberFormat.EMPTY));
                String healthStr = text.getString().replaceAll("\\D", "");

                 
                if (healthStr.isEmpty()) {
                    return fallbackHealth;  
                }

                try {
                    return Float.parseFloat(healthStr);
                } catch (NumberFormatException e) {
                     
                    System.err.println("Failed to parse health string: " + healthStr + " for player: " + player.getName().getString());
                    return fallbackHealth;  
                }
            }
        }

        return fallbackHealth;  
    }
}