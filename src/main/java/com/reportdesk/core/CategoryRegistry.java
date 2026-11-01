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

package com.reportdesk.core;
import java.util.ArrayList; import java.util.Collection; import java.util.Comparator; import java.util.LinkedHashMap; import java.util.List; import java.util.Locale; import java.util.Map; import java.util.Optional;
/**
* Lookup helper for categories and their command aliases.
*/ public final class CategoryRegistry {


private final Map<String, ReportCategory> byId;

 private final Map<String, ReportCategory> byAlias;


private CategoryRegistry(Map<String, ReportCategory> byId, Map<String, ReportCategory> byAlias) {



 this.byId = byId;



 this.byAlias = byAlias;

 }


public static CategoryRegistry of(Collection<ReportCategory> categories) {



 Map<String, ReportCategory> ids = new LinkedHashMap<>();



 Map<String, ReportCategory> aliases = new LinkedHashMap<>();



 if (categories != null) {





 for (ReportCategory category : categories) {







 if (category == null) {









 continue;







 }







 ids.put(normalize(category.id()), category);







 for (String alias : category.aliases()) {









 if (alias != null && !alias.isBlank()) {











 aliases.put(normalize(alias), category);









 }







 }





 }



 }



 if (ids.isEmpty()) {





 ReportCategory fallback = new ReportCategory("other", "Other", "PAPER",







 List.of("misc"), true, 0, List.of());





 ids.put("other", fallback);





 aliases.put("misc", fallback);



 }



 return new CategoryRegistry(ids, aliases);

 }


public Optional<ReportCategory> find(String idOrAlias) {



 if (idOrAlias == null || idOrAlias.isBlank()) {





 return Optional.empty();



 }



 String key = normalize(idOrAlias);



 ReportCategory byIdMatch = byId.get(key);



 if (byIdMatch != null) {





 return Optional.of(byIdMatch);



 }



 return Optional.ofNullable(byAlias.get(key));

 }


public boolean isEnabled(String idOrAlias) {



 return find(idOrAlias).map(ReportCategory::enabled).orElse(false);

 }


public ReportCategory getOrDefault(String idOrAlias, String defaultId) {



 return find(idOrAlias)





 .or(() -> find(defaultId))





 .orElseGet(() -> byId.values().iterator().next());

 }


public List<ReportCategory> sorted() {



 List<ReportCategory> categories = new ArrayList<>(byId.values());



 categories.sort(Comparator.comparingInt(ReportCategory::priority).reversed()





 .thenComparing(ReportCategory::id));



 return List.copyOf(categories);

 }


public List<ReportCategory> enabledSorted() {



 return sorted().stream().filter(ReportCategory::enabled).toList();

 }


public List<String> commandSuggestions() {



 List<String> suggestions = new ArrayList<>();



 for (ReportCategory category : enabledSorted()) {





 suggestions.add(category.id());





 suggestions.addAll(category.aliases());



 }



 return List.copyOf(suggestions);

 }


public int size() {



 return byId.size();

 }


private static String normalize(String value) {



 return value.trim().toLowerCase(Locale.ROOT);

 } }
