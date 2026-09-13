package dev.darkvisuals.client.managers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

 public class HitDetectionManager {
    
    private static HitDetectionManager instance;
    
     
    private static final long HIT_TOLERANCE_NANOS = 5_000_000L;  
    
     
    private final Map<String, Long> lastHitTimes = new ConcurrentHashMap<>();
    
    private HitDetectionManager() {}
    
    public static HitDetectionManager getInstance() {
        if (instance == null) {
            instance = new HitDetectionManager();
        }
        return instance;
    }
    
     public boolean canProcessHit(PlayerEntity attacker, Entity target) {
        if (attacker == null || target == null) {
            return true;  
        }
        
        String hitKey = generateHitKey(attacker, target);
        long currentTime = System.nanoTime();
        
        Long lastHitTime = lastHitTimes.get(hitKey);
        if (lastHitTime == null) {
             
            return true;
        }
        
        long timeDifference = currentTime - lastHitTime;
        
        if (timeDifference >= HIT_TOLERANCE_NANOS) {
             
            return true;
        }
        
         
        return false;
    }
    
     public void registerHit(PlayerEntity attacker, Entity target) {
        if (attacker == null || target == null) {
            return;
        }
        
        String hitKey = generateHitKey(attacker, target);
        lastHitTimes.put(hitKey, System.nanoTime());
    }
    
     public void cleanup() {
        long currentTime = System.nanoTime();
        long maxAge = 1_000_000_000L;  
        
        lastHitTimes.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > maxAge
        );
    }
    
     private String generateHitKey(PlayerEntity attacker, Entity target) {
        return attacker.getUuid().toString() + "->" + target.getUuid().toString();
    }
    
     public long getLastHitTime(PlayerEntity attacker, Entity target) {
        String hitKey = generateHitKey(attacker, target);
        Long lastHitTime = lastHitTimes.get(hitKey);
        return lastHitTime != null ? lastHitTime : 0L;
    }
    
     public double getTimeSinceLastHit(PlayerEntity attacker, Entity target) {
        String hitKey = generateHitKey(attacker, target);
        Long lastHitTime = lastHitTimes.get(hitKey);
        if (lastHitTime == null) {
            return Double.MAX_VALUE;  
        }
        return (System.nanoTime() - lastHitTime) / 1_000_000.0;  
    }
    
     public int getActiveHitRecords() {
        return lastHitTimes.size();
    }
}
