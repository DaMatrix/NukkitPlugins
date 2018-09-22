package net.daporkchop.floatingparticles;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.math.Vector3;

import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;

/**
 * @author DaPorkchop_
 */
public class ParticleCommand extends Command {
    public ParticleCommand() {
        super("floatingparticle", "", null, new String[]{"fp"});
        this.commandParameters.clear();
        this.commandParameters.put("default", new CommandParameter[]{});
        this.commandParameters.put("particle", new CommandParameter[]{
                new CommandParameter("type", false, new String[]{
                        "AngryVillager",
                        "Explode",
                        "Flame",
                        "HappyVillager",
                        "Heart",
                        "HugeExplode",
                        "Ink",
                        "InstantEnchant",
                        "InstantSpell",
                        "LavaDrip",
                        "Lava",
                        "Portal",
                        "RainSplash",
                        "Redstone",
                        "Smoke",
                        "Splash",
                        "Spore",
                        "WaterDrip",
                        "Water"
                })
        });
        this.commandParameters.put("clear", new CommandParameter[]{
                new CommandParameter("clear", false, new String[]{
                        "clear"
                })
        });
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        Map<Level, Collection<Particle>> particleMap = FloatingParticles.INSTANCE.getParticles();
        Level level;
        if (sender instanceof Player)   {
            level = ((Player) sender).level;
        } else {
            sender.sendMessage("§cThis command must be run as a player!");
            return false;
        }
        Collection<Particle> particles = particleMap.computeIfAbsent(level, a -> new ArrayDeque<>());
        String particleName = FloatingParticles.INSTANCE.getDefaultParticleName();
        if (args.length == 1)   {
            particleName = args[0];
        }
        if ("clear".equals(particleName))   {
            particleMap.remove(level);
            sender.sendMessage(String.format("Removed %d particles in world %s", particles.size(), level.getName()));
            return true;
        }
        try {
            Class<?> clazz = Class.forName(String.format("cn.nukkit.level.particle.%sParticle", particleName), true, Particle.class.getClassLoader());
            Constructor constructor = clazz.getConstructor(Vector3.class);
            Position position = (Player) sender;
            particles.add((Particle) constructor.newInstance(new Vector3(position.x, position.y, position.z)));
            sender.sendMessage(String.format("Added particle of type %s to world %s", particleName, level.getName()));
        } catch (Exception e)  {
            sender.sendMessage(String.format("§cInvalid particle name: %s", particleName));
        }
        return true;
    }
}
