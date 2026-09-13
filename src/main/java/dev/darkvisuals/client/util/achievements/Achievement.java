package dev.darkvisuals.client.util.achievements;

public class Achievement {
    public final String code;
    public final String name;
    public final String description;
    public final String imageUrl;
    public final String unlockFeature;
    public final String grantedAt;

    public Achievement(String code, String name, String description,
                       String imageUrl, String unlockFeature, String grantedAt) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.unlockFeature = unlockFeature;
        this.grantedAt = grantedAt;
    }
}