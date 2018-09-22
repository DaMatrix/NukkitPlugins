package net.daporkchop.floatingparticles;


import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.Utils;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

@Getter
public final class FloatingParticles extends PluginBase {
    public static FloatingParticles INSTANCE;

    {
        INSTANCE = this;
    }

    private final Map<Level, Collection<Particle>> particles = new Hashtable<>();
    private String defaultParticleName = "Flame";

    @Override
    public void onEnable() {
        this.saveResource("positions.json");
        this.saveResource("config.yml");

        this.reloadConfig();

        this.getServer().getScheduler().scheduleRepeatingTask(this, new Task() {
            @Override
            public void onRun(int currentTick) {
                FloatingParticles.this.particles.forEach((level, particles) -> particles.forEach(level::addParticle));
            }
        }, 5);

        this.getServer().getCommandMap().register("floatingparticles", new ParticleCommand());
    }

    @Override
    public void onDisable() {
        JsonArray array = new JsonArray();
        this.particles.forEach((level, particles) -> {
            JsonObject levelObject = new JsonObject();
            array.add(levelObject);
            levelObject.addProperty("name", level.getName());
            JsonArray particleArray = new JsonArray();
            levelObject.add("particles", particleArray);
            particles.forEach(particle -> {
                JsonObject particleObject = new JsonObject();
                particleArray.add(particleObject);
                particleObject.addProperty("x", particle.x);
                particleObject.addProperty("y", particle.y);
                particleObject.addProperty("z", particle.z);
                particleObject.addProperty("class", particle.getClass().getCanonicalName());
            });
        });
        JsonObject object = new JsonObject();
        object.add("levels", array);
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(object);
        try {
            Utils.writeFile(new File(this.getDataFolder(), "positions.json"), json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.particles.clear();
    }

    @Override
    public void reloadConfig() {
        File dataFolder = this.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException(String.format("Unable to create directory %s!", dataFolder.getAbsolutePath()));
        }

        Config config = new Config(
                new File(dataFolder, "config.yml")
        );
        if (!config.isCorrect()) {
            throw new IllegalStateException();
        }
        this.defaultParticleName = config.getString("defaultParticleName", "Flame");
        config.save();

        this.particles.clear();
        try {
            JsonObject object = new JsonParser().parse(Utils.readFile(new File(dataFolder, "positions.json"))).getAsJsonObject();
            object.getAsJsonArray("levels").forEach(element -> {
                JsonObject levelObject = element.getAsJsonObject();
                String name = levelObject.get("name").getAsString();
                Level level = this.getServer().getLevelByName(name);
                if (level == null)  {
                    throw new IllegalStateException("Invalid world name: " + name);
                }
                Collection<Particle> particles = this.particles.computeIfAbsent(level, a -> new ArrayDeque<>());
                levelObject.getAsJsonArray("particles").forEach(particleElement -> {
                    JsonObject particleObject = particleElement.getAsJsonObject();
                    double x = particleObject.get("x").getAsDouble();
                    double y = particleObject.get("y").getAsDouble();
                    double z = particleObject.get("z").getAsDouble();
                    String className = particleObject.get("class").getAsString();
                    try {
                        Class<?> clazz = Class.forName(className, true, Particle.class.getClassLoader());
                        Constructor constructor = clazz.getConstructor(Vector3.class);
                        particles.add((Particle) constructor.newInstance(new Vector3(x, y, z)));
                    } catch (Exception e)   {
                        throw new RuntimeException(e);
                    }
                });
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
