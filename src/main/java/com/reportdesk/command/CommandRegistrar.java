/*
 * ReportDesk - Enterprise-grade player reporting and evidence system.
 * Copyright (C) 2026 William
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.reportdesk.command;
import org.bukkit.Bukkit; import org.bukkit.command.Command; import org.bukkit.command.CommandExecutor; import org.bukkit.command.CommandMap; import org.bukkit.command.CommandSender; import org.bukkit.command.PluginCommand; import org.bukkit.command.TabCompleter; import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Field; import java.lang.reflect.Method; import java.util.ArrayList; import java.util.List;
/**
* Registers commands for both plugin.yml and paper-plugin.yml deployments.
*
* <p>Paper plugins do not read commands from paper-plugin.yml. If plugin.yml
* command resolution is unavailable, this falls back to the server CommandMap
* (public getCommandMap() first, then the commandMap field). Registration only
* happens once during onEnable.</p>
*/ public final class CommandRegistrar {


private CommandRegistrar() {

 }


public static void register(



 JavaPlugin plugin,



 String name,



 CommandExecutor executor,



 TabCompleter completer,



 List<String> aliases,



 String description,



 String usage

 ) {



 PluginCommand existing = findCommand(plugin, name);



 if (existing != null) {





 existing.setExecutor(executor);





 existing.setTabCompleter(completer);





 existing.setDescription(description);





 existing.setUsage(usage);





 plugin.getLogger().info("Registered /" + name + " via plugin.yml");





 return;



 }




try {





 CommandMap commandMap = commandMap(plugin);





 List<String> allAliases = aliases == null ? List.of() : List.copyOf(aliases);





 BukkitCommand command = new BukkitCommand(name, executor, completer);





 command.setDescription(description);





 command.setUsage(usage);





 command.setAliases(allAliases);





 commandMap.register("reportdesk", command);






for (String alias : allAliases) {







 BukkitCommand aliasCommand = new BukkitCommand(alias, executor, completer);







 aliasCommand.setDescription(description);







 aliasCommand.setUsage(usage);







 commandMap.register("reportdesk", aliasCommand);





 }





 plugin.getLogger().info("Registered /" + name + " via CommandMap");



 } catch (Throwable throwable) {





 plugin.getLogger().warning("Failed to register /" + name + " command: "







 + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());



 }

 }


/**


* Returns a descriptor-declared command, or null when the plugin is a


* paper-plugin. Paper throws {@link UnsupportedOperationException} from


* {@link JavaPlugin#getCommand(String)} instead of returning null, so a


* plain null check is not enough.


*/

 public static PluginCommand findCommand(JavaPlugin plugin, String name) {



 try {





 return plugin.getCommand(name);



 } catch (RuntimeException exception) {





 return null;



 }

 }


private static CommandMap commandMap(JavaPlugin plugin) throws Exception {



 Object server = Bukkit.getServer();



 try {





 Method method = server.getClass().getMethod("getCommandMap");





 Object value = method.invoke(server);





 if (value instanceof CommandMap map) {







 return map;





 }



 } catch (ReflectiveOperationException ignored) {





 // Fall through to field lookup.



 }




List<String> tried = new ArrayList<>();



 Class<?> type = server.getClass();



 while (type != null && type != Object.class) {





 for (Field field : type.getDeclaredFields()) {







 if (!"commandMap".equals(field.getName())) {









 continue;







 }







 tried.add(type.getName() + "." + field.getName());







 try {









 field.setAccessible(true);









 Object value = field.get(server);









 if (value instanceof CommandMap map) {











 return map;









 }







 } catch (ReflectiveOperationException | RuntimeException ignored) {









 // Try the next candidate.







 }





 }





 type = type.getSuperclass();



 }



 throw new IllegalStateException("No Bukkit CommandMap found; tried " + tried);

 }


private static final class BukkitCommand extends Command {




private final CommandExecutor executor;



 private final TabCompleter completer;




private BukkitCommand(String name, CommandExecutor executor, TabCompleter completer) {





 super(name);





 this.executor = executor;





 this.completer = completer;



 }




@Override



 public boolean execute(CommandSender sender, String commandLabel, String[] args) {





 return executor.onCommand(sender, this, commandLabel, args);



 }




@Override



 public List<String> tabComplete(CommandSender sender, String alias, String[] args) {





 return completer == null ? List.of() : completer.onTabComplete(sender, this, alias, args);



 }

 } }
