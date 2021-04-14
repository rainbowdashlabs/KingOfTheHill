package de.eldoria.kingofthehill;

import de.eldoria.kingofthehill.config.Configuration;
import de.eldoria.kingofthehill.listener.CommandListener;
import de.eldoria.kingofthehill.scheduler.KingChecker;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KingOfTheHill {
    private static KingOfTheHill instance;
    private ShardManager shardManager;
    private Configuration configuration;
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

    public KingOfTheHill() {
    }

    public static void main(String[] args) throws IOException, LoginException {
        instance = new KingOfTheHill();
        instance.start();
    }

    public void start() throws IOException, LoginException {
        configuration = Configuration.load();
        initJDA();
        shardManager.addEventListener(new CommandListener(configuration));
        KingChecker kingChecker = new KingChecker(shardManager, configuration);
        shardManager.addEventListener(kingChecker);
        service.scheduleAtFixedRate(kingChecker, 10, 60, TimeUnit.SECONDS);

        //LocalTime now = LocalTime.now();
        //int hour = 60 * 60;
        //int seconds = ((4 - now.getMinute() % 5) * 60) + (60 - now.getSecond());
        //log.info("Scheduled next reduction in {} seconds.", seconds);
        //service.scheduleAtFixedRate(new PointReducer(configuration), seconds, 300, TimeUnit.SECONDS);
    }

    public void initJDA() throws LoginException {
        shardManager = DefaultShardManagerBuilder
                .create(
                        configuration.getToken(),
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_EMOJIS,
                        GatewayIntent.GUILD_VOICE_STATES)
                .setMaxReconnectDelay(60)
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
                .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                .setBulkDeleteSplittingEnabled(false)
                .build();

        try {
            shardManager.getShardById(0).awaitReady();
        } catch (InterruptedException e) {
            log.info("Could not await ready.");
        }

        log.info("{} shards initialized", shardManager.getShardsTotal());
    }
}
