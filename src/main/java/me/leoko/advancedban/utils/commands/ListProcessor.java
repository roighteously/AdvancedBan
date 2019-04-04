package me.leoko.advancedban.utils.commands;

import me.leoko.advancedban.MethodInterface;
import me.leoko.advancedban.Universal;
import me.leoko.advancedban.manager.MessageManager;
import me.leoko.advancedban.utils.Command;
import me.leoko.advancedban.utils.Punishment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static me.leoko.advancedban.utils.CommandUtils.processName;

public class ListProcessor implements Consumer<Command.CommandInput> {
    private Function<String, List<Punishment>> listSupplier;
    private String config;
    private boolean history;
    private boolean hasTarget;

    public ListProcessor(Function<String, List<Punishment>> listSupplier, String config, boolean history, boolean hasTarget) {
        this.listSupplier = listSupplier;
        this.config = config;
        this.history = history;
        this.hasTarget = hasTarget;
    }

    @Override
    public void accept(Command.CommandInput input) {
        String target = "";
        if (hasTarget) {
            target = input.getPrimary();
            if (!target.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
                target = processName(input);
                if (target == null)
                    return;
            } else {
                input.next();
            }
        }

        MethodInterface mi = Universal.get().getMethods();
        final List<Punishment> punishments = listSupplier.apply(target);
        if (punishments.isEmpty()) {
            MessageManager.sendMessage(input.getSender(), config + ".NoEntries",
                    true, "NAME", target);
            return;
        }

        for (Punishment punishment : punishments)
            if (punishment.isExpired())
                punishment.delete();

        int page = input.hasNext() ? Integer.parseInt(input.getPrimary()) : 1;
        if (punishments.size() / 5.0 + 1 <= page) {
            MessageManager.sendMessage(input.getSender(), config + ".OutOfIndex",
                    true, "PAGE", page + "");
            return;
        }

        String prefix = MessageManager.getMessage("General.Prefix");
        List<String> header = MessageManager.getLayout(mi.getMessages(), config + ".Header",
                "PREFIX", prefix, "NAME", target);

        for (String line : header)
            mi.sendMessage(input.getSender(), line);

        SimpleDateFormat format = new SimpleDateFormat(mi.getString(mi.getConfig(),
                "DateFormat", "dd.MM.yyyy-HH:mm"));

        for (int i = (page - 1) * 5; i < page * 5 && punishments.size() > i; i++) {
            Punishment punishment = punishments.get(i);
            List<String> entryLayout = MessageManager.getLayout(mi.getMessages(), config + ".Entry",
                    "PREFIX", prefix,
                    "NAME", punishment.getName(),
                    "DURATION", punishment.getDuration(history),
                    "OPERATOR", punishment.getOperator(),
                    "REASON", punishment.getReason(),
                    "TYPE", punishment.getType().getConfSection(),
                    "ID", punishment.getId() + "",
                    "DATE", format.format(new Date(punishment.getStart())));

            for (String line : entryLayout)
                mi.sendMessage(input.getSender(), line);
        }

        MessageManager.sendMessage(input.getSender(), config + ".Footer", false,
                "CURRENT_PAGE", page + "",
                "TOTAL_PAGES", (punishments.size() / 5 + (punishments.size() % 5 != 0 ? 1 : 0)) + "",
                "COUNT", punishments.size() + "");
        if (punishments.size() / 5.0 + 1 > page + 1) {
            MessageManager.sendMessage(input.getSender(), config + ".PageFooter", false,
                    "NEXT_PAGE", (page + 1) + "", "NAME", target);
        }
    }
}
