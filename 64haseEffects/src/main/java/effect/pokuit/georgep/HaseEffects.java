package effect.pokuit.georgep;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by George on 18/01/14.
 * <p/>
 * Purpose Of File:
 * <p/>
 * Latest Change:
 */
public class HaseEffects extends JavaPlugin implements TabCompleter {

    FileConfiguration config = null;

    File dataFile = null;
    FileConfiguration data = null;

    int i = 0;

    public void onEnable() {
        saveDefaultConfig();
        this.config = getConfig();

        dataFile = new File(getDataFolder(), "data.yml");

        if(!dataFile.exists()) saveResource("data.yml", false);
        data = YamlConfiguration.loadConfiguration(dataFile);

        runAllEffects();
    }

    public void onDisable() {

    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /particles set/remove <EffectType (If set)> [<x>] [<y>] [<z>] [[<world>]]
        if(args.length == 0) {
            sender.sendMessage(getFormat("formats.NotEnoughArguments", true));
            return false;
        }

        if(args[0].equalsIgnoreCase("set")) {
            Effect e;
            Location loc;
            try {
                e = Effect.valueOf(args[1]);
            } catch(Exception ex) {
                sender.sendMessage(getFormat("formats.InvalidEffect", true));
                return true;
            }
            if(sender instanceof Player) {
                Player p = (Player)sender;
                if(args.length == 5) { loc = getLocation(args[2], args[3], args[4], p.getWorld().getName()); }
                else { loc = new Location(p.getWorld(), p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ()); }
            } else {
                if(args.length == 6) { loc = getLocation(args[2], args[3], args[4], args[5]); }
                else {
                    sender.sendMessage(getFormat("formats.NoLocation", true));
                    return false;
                }
            }

            if(loc == null) {
                sender.sendMessage(getFormat("formats.NotValidLocation", true));
                return false;
            }
            playEffect(loc, e);
            sender.sendMessage(getFormat("formats.SuccessfulSet", true));
            return true;
        } else {
            if(args[0].equalsIgnoreCase("remove")) {
                Location loc;
                if(sender instanceof Player) {
                    Player p = (Player)sender;
                    if(args.length == 4) { loc = getLocation(args[1], args[2], args[3], p.getWorld().getName()); }
                    else { loc = new Location(p.getWorld(), p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ()); }
                } else {
                    if(args.length == 5) { loc = getLocation(args[1], args[2], args[3], args[4]); }
                    else {
                        sender.sendMessage(getFormat("formats.NoLocation", true));
                        return false;
                    }
                }
                if(loc == null) {
                    sender.sendMessage(getFormat("formats.NotValidLocation", true));
                    return false;
                }
                removeEffect(loc);
                sender.sendMessage(getFormat("formats.SuccessfulRemove", true));
                return true;
            }

            sender.sendMessage(getFormat("formats.InvalidWildCard", true));
            return false;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if(args.length == 2 && args[0].equalsIgnoreCase("set")) {
            List<String> effects = new ArrayList<String>();
            for(Effect fx : Effect.values()) {
                effects.add(fx.toString());
            }
            return effects;
        }
        return new ArrayList<String>();
    }

    public Location getLocation(String x, String y, String z, String world) {
        Double xLoc;
        Double yLoc;
        Double zLoc;
        try {
            xLoc = Double.parseDouble(x);
            yLoc = Double.parseDouble(y);
            zLoc = Double.parseDouble(z);
        } catch(NumberFormatException ex) {
            return null;
        }
        try {
            World w = Bukkit.getServer().getWorld(world);
            return new Location(w, xLoc, yLoc, zLoc);
        } catch(NullPointerException ex) {
            return null;
        }
    }

    public String getFormat(String s, boolean b, String[]... changes) {
        String format = (b ? getFormat("prefix", false)+" " : "")+ChatColor.translateAlternateColorCodes('&', config.getString(s, ""));
        for(String[] ch: changes) {
            if(ch.length != 2) continue;
            format = format.replace(ch[0], ch[1]);
        }
        return format;
    }

    public void playEffect(final Location l, final Effect e) {
        new BukkitRunnable() {

            @Override
            public void run() {
                if(l.getChunk().isLoaded()) l.getWorld().playEffect(l, e, 0);
            }

        }.runTaskTimer(this, 0, config.getInt("delay"));

        String pre = i+"~"+l.getBlockX()+"~"+l.getBlockY()+"~"+l.getBlockZ()+"~"+l.getWorld().getName()+"~"+e.toString();
        data.set(pre+".loc.x", l.getBlockX());
        data.set(pre+".loc.y", l.getBlockY());
        data.set(pre+".loc.z", l.getBlockZ());
        data.set(pre+".loc.world", l.getWorld().getName());

        data.set(pre+".effectType", e.toString());

        try {
            data.save(dataFile);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        i++;
    }

    public void removeEffect(Location l) {
        for(String s : data.getKeys(false)) {
            if(data.getInt(s+".loc.x") == l.getBlockX() &&
                    data.getInt(s+".loc.y") == l.getBlockY() &&
                    data.getInt(s+".loc.z") == l.getBlockZ() &&
                    data.getString(s + ".loc.world").equals(l.getWorld().getName())) {
                getLogger().info("test");
                data.set(s, null);
                try {
                    data.save(dataFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void runAllEffects() {
        for(final String s : data.getKeys(false)) {
            final Location l = new Location(
                    Bukkit.getServer().getWorld(data.getString(s+".loc.world")),
                    data.getDouble(s+".loc.x"),
                    data.getDouble(s+".loc.y"),
                    data.getDouble(s+".loc.z")
            );
            Effect e;
            try {
                e = Effect.valueOf(data.getString(s+".effectType"));
            } catch(IllegalArgumentException ex) {
                getLogger().info(getFormat("formats.InvalidDataFile", true));
                continue;
            }
            final Effect fe = e;

            new BukkitRunnable() {

                @Override
                public void run() {
                    if(l.getChunk().isLoaded()) l.getWorld().playEffect(l, fe, 0);
                }

            }.runTaskTimer(this, 0, config.getInt("delay"));
        }
    }
}
