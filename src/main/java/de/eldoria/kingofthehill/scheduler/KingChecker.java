package de.eldoria.kingofthehill.scheduler;

import de.eldoria.kingofthehill.config.Configuration;
import de.eldoria.kingofthehill.config.UserStat;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class KingChecker extends ListenerAdapter implements Runnable {
    private final ShardManager shardManager;
    private final Configuration configuration;
    private Instant nextKick;

    public KingChecker(ShardManager shardManager, Configuration configuration) {
        this.shardManager = shardManager;
        this.configuration = configuration;

        VoiceChannel channel = shardManager.getVoiceChannelById(configuration.getVoiceChannel());
        if (channel == null) {
            log.error("Channel not found.");
            return;
        }

        Guild guild = channel.getGuild();

        List<Member> members = channel.getMembers();

        if (members.isEmpty()) {
            log.debug("Channel is empty");
            return;
        }

        Member currentChallenger = members.get(0);
        UserStat userStat = configuration.getStats().get(members.get(0).getUser().getIdLong());

        nextKick = getNextKick(userStat != null ? userStat.getPercent() : 0);
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        if (event.getChannelJoined().getIdLong() == configuration.getVoiceChannel()) {
            if (ThreadLocalRandom.current().nextInt(0, 101) > 90) {
                event.getGuild().kickVoiceMember(event.getMember()).queue();
                return;
            }

            UserStat userStat = configuration.getStats().get(event.getMember().getUser().getIdLong());
            log.info("User {} joined.", event.getMember().getIdLong());
            nextKick = getNextKick(userStat != null ? userStat.getPercent() : 0);
        }
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        if (event.getChannelLeft().getIdLong() == configuration.getVoiceChannel()) {
            log.info("User {} left", event.getMember().getIdLong());
        }
    }

    @Override
    public void run() {
        VoiceChannel channel = shardManager.getVoiceChannelById(configuration.getVoiceChannel());
        if (channel == null) {
            log.error("Channel not found.");
            return;
        }

        Guild guild = channel.getGuild();

        List<Member> members = channel.getMembers();

        if (members.isEmpty()) {
            log.debug("Channel is empty");
            return;
        }

        Member currentChallenger = members.get(0);

        configuration.upcount(currentChallenger.getUser().getIdLong());

        configuration.save();


        if (Instant.now().isAfter(nextKick)) {
            guild.kickVoiceMember(currentChallenger).queue();
            log.debug("Kicked {}", currentChallenger.getIdLong());
        }

        long kingId = configuration.getKing();

        Role kingRole = guild.getRoleById(configuration.getRole());

        if (kingRole == null) {
            log.error("Role not found");
            return;
        }

        Member king = guild.getMemberById(kingId);

        if (king == null) {
            log.debug("The current king is no longer on this guild.");
            return;
        }

        // Check if king is already king
        if (king.getRoles().contains(kingRole)) {
            log.debug("King is already king");
            return;
        }

        List<Member> membersWithRoles = guild.getMembersWithRoles(kingRole);
        for (Member membersWithRole : membersWithRoles) {
            guild.removeRoleFromMember(membersWithRole, kingRole).queue();
            log.debug("Role removed.");
        }

        log.info("New king assigned.");
        guild.addRoleToMember(king, kingRole).queue();
    }

    private Instant getNextKick(double percents) {
        double clamped = Math.max(20, Math.min(80, percents));
        double percent = 20 / clamped;
        long minutes = (long) (percent * ThreadLocalRandom.current().nextInt(15, 25));
        log.debug("Next kick in {} minutes", minutes);
        return Instant.now().plus(minutes, ChronoUnit.MINUTES);
    }
}
