package de.eldoria.kingofthehill.listener;

import de.eldoria.kingofthehill.config.Configuration;
import de.eldoria.kingofthehill.config.UserStat;
import de.eldoria.kingofthehill.util.TextFormatting;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class CommandListener extends ListenerAdapter {
    private final Configuration configuration;
    private int rebellionLevel = 0;

    public CommandListener(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != configuration.getGameChannel()) return;

        if ("👑".equalsIgnoreCase(event.getMessage().getContentRaw())) {
            @Nullable UserStat king = configuration.getKingData();
            if (king == null) {
                event.getChannel().sendMessage("Es gibt derzeit keinen König.").queue();
                return;
            }
            Member kingMember = event.getGuild().getMemberById(king.getId());
            if (kingMember == null) {
                event.getChannel().sendMessage("Es gibt derzeit keinen König.").queue();
                return;
            }
            String message = "**" + kingMember.getAsMention() + "** ist aktuell King/Queen of the Hill mit **"
                    + String.format("%.2f", king.getPercent()) + "% Dominanz**!";
            if (kingMember.getIdLong() != event.getAuthor().getIdLong()) {
                UserStat userStat = configuration.getStats().get(event.getAuthor().getIdLong());
                message += "\nDu hast derzeit **" + (userStat == null ? 0 : String.format("%.2f", userStat.getPercent())) + "% Dominanz**.";
            }
            event.getChannel().sendMessage(message).allowedMentions(Collections.emptyList()).queue();
            return;
        }

        if ("📜".equalsIgnoreCase(event.getMessage().getContentRaw())) {
            List<UserStat> values = new ArrayList<>(configuration.getStats().values());
            Collections.sort(values);
            Collections.reverse(values);
            values = values.subList(0, Math.min(values.size(), 10));
            var tableBuilder = TextFormatting.getTableBuilder(values, "", "Name", "Dominanz");
            int rank = 1;
            for (UserStat value : values) {
                Member memberById = event.getGuild().getMemberById(value.getId());
                String name = memberById == null ? "Unbekannt" : memberById.getEffectiveName();
                tableBuilder.setNextRow(String.valueOf(rank), name, String.format("%.2f", value.getPercent()));
                rank++;
            }

            event.getChannel().sendMessage(tableBuilder.toString()).queue();
            return;
        }

        if ("⚔️".equalsIgnoreCase(event.getMessage().getContentRaw()) || "⚔".equalsIgnoreCase(event.getMessage().getContentRaw())) {
            VoiceChannel channel = event.getJDA().getVoiceChannelById(configuration.getVoiceChannel());
            if (channel == null) {
                log.error("Channel not found.");
                return;
            }

            Guild guild = channel.getGuild();

            List<Member> members = channel.getMembers();

            if (members.isEmpty()) {
                log.debug("Channel is empty");
                event.getChannel().sendMessage("Stell dir vor es ist Rebellion, aber keiner geht hin.").queue();
                return;
            }

            Member currentChallenger = members.get(0);
            UserStat userStat = configuration.getStats().get(event.getAuthor().getIdLong());
            if (userStat == null || userStat.getPoints() < 10) {
                event.getChannel().sendMessage("Dafür hast du nicht genug Punkte.").queue();
                return;
            }
            userStat.reduce(10);
            configuration.save();
            if (currentChallenger.getIdLong() == event.getAuthor().getIdLong()) {
                event.getChannel().sendMessage("Du bezahlst ein paar Raufbolde eine Rebellion gegen dich anzufeuern.\nInteresant...").queue();
                return;
            }

            int chance = ThreadLocalRandom.current().nextInt(0, 100);
            if (chance > 90 - rebellionLevel) {

                guild.kickVoiceMember(currentChallenger).queue();
                event.getChannel().sendMessage("Die Rebellion war erfolgreich!\n" +
                        "Der König ist gestürzt! Lang lebe der König!").queue();
                rebellionLevel = 0;
                return;
            }
            event.getChannel().sendMessage("Die Rebellion ist noch nicht stark genug. Aber du hast einige Verbündete gewonnen.").queue();
            rebellionLevel += 5;
            return;
        }
    }
}
