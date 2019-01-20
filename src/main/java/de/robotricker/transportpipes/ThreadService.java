package de.robotricker.transportpipes;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import de.robotricker.transportpipes.duct.Duct;
import de.robotricker.transportpipes.duct.DuctRegister;
import de.robotricker.transportpipes.duct.manager.GlobalDuctManager;
import de.robotricker.transportpipes.location.BlockLocation;
import de.robotricker.transportpipes.log.LoggerService;
import de.robotricker.transportpipes.log.SentryService;
import de.robotricker.transportpipes.protocol.ProtocolService;
import de.robotricker.transportpipes.utils.Constants;
import de.robotricker.transportpipes.utils.WorldUtils;

public class ThreadService extends Thread {

    private final Map<Runnable, Long> tasks;

    private JavaPlugin plugin;
    private LoggerService logger;
    private SentryService sentry;
    private ProtocolService protocol;
    private GlobalDuctManager globalDuctManager;
    private DuctRegister ductRegister;

    private boolean running = false;
    private int preferredTPS = 10;
    private int currentTPS = 0;

    @Inject
    public ThreadService(JavaPlugin plugin, LoggerService logger, SentryService sentry, ProtocolService protocol, GlobalDuctManager globalDuctManager, DuctRegister ductRegister) {
        super("TransportPipes-Thread");
        this.plugin = plugin;
        this.logger = logger;
        this.sentry = sentry;
        this.protocol = protocol;
        this.globalDuctManager = globalDuctManager;
        this.ductRegister = ductRegister;
        this.tasks = Collections.synchronizedMap(new LinkedHashMap<>());

        Bukkit.getScheduler().runTaskTimer(plugin, this::tickDuctSpawnAndDespawn, 20L, 20L);
    }

    @Override
    public void run() {
        logger.info("Started ThreadService");
        running = true;
        sentry.addTag("thread", getName());
        sentry.injectThread(this);

        long lastTick = System.currentTimeMillis();
        long lastSec = lastTick;
        int tpsCounter = 0;
        while (running) {
            long currentTick = System.currentTimeMillis();
            long diff = currentTick - lastTick;
            if (diff >= 1000f / preferredTPS) {

                tick();

                tpsCounter++;
                lastTick = currentTick;

                if (currentTick - lastSec >= 1000) {
                    currentTPS = tpsCounter;
                    tpsCounter = 0;
                    lastSec = currentTick;
                    logger.debug("TPS: " + currentTPS);
                }

            } else {
                long waitTime = (long) (1000f / preferredTPS - diff);
                try {
                    sleep(waitTime);
                } catch (InterruptedException e) {
                    logger.error("ThreadService was terminated while sleeping!", e);
                }
            }
        }
        logger.info("Stopped ThreadService");
    }

    public Map<Runnable, Long> getTasks() {
        return tasks;
    }

    private void tick() {
        //schedule tasks
        Set<Runnable> taskSet = tasks.keySet();
        synchronized (tasks) {
            Iterator<Runnable> taskIt = taskSet.iterator();
            while (taskIt.hasNext()) {
                Runnable task = taskIt.next();
                if (tasks.get(task) > 1) {
                    tasks.put(task, tasks.get(task) - 1);
                } else {
                    taskIt.remove();
                    task.run();
                }
            }
        }

        globalDuctManager.tick();
    }

    private void tickDuctSpawnAndDespawn() {
        synchronized (globalDuctManager.getDucts()) {
            for (World world : Bukkit.getWorlds()) {
                Map<BlockLocation, Duct> ductMap = globalDuctManager.getDucts(world);
                if (ductMap != null) {
                    for (Duct duct : ductMap.values()) {
                        List<Player> playerList = WorldUtils.getPlayerList(world);
                        for (Player worldPlayer : playerList) {
                            if (duct.getBlockLoc().toLocation(world).distance(worldPlayer.getLocation()) <= Constants.DEFAULT_RENDER_DISTANCE) {
                                // spawn globalDuctManager if not there
                                duct.getDuctType().getBaseDuctType().getDuctManager().notifyDuctShown(duct, worldPlayer);
                            } else {
                                // despawn globalDuctManager if there
                                duct.getDuctType().getBaseDuctType().getDuctManager().notifyDuctHidden(duct, worldPlayer);
                            }
                        }
                    }
                }
            }
        }
    }

    public int getCurrentTPS() {
        return currentTPS;
    }

    public int getPreferredTPS() {
        return preferredTPS;
    }

    public void setPreferredTPS(int preferredTPS) {
        this.preferredTPS = preferredTPS;
    }

    public boolean isRunning() {
        return running;
    }

    public void stopRunning() {
        running = false;
    }

}
