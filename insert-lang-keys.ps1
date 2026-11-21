# Inserts the P1#4 (assign/transfer + per-staff stats) message keys into every lang file.
# NOTE: this sandbox parser rejects a bare command as the right operand of '+',
# so all concatenation uses quoted string operands only.
$root = 'src\main\resources\lang'

$adminKeys = @{
  en_US = @(
    "  assigned: '" + '<green>Assigned report <white>#%id%</white> to <white>%staff%</white>.' + "'"
    "  already-assigned: '" + '<yellow>Report <white>#%id%</white> is already handled by <white>%staff%</white>; nothing changed.' + "'"
    "  assign-target-offline: '" + '<red>Admin <white>%admin%</white> is not online. The assignment target must be online.' + "'"
    "  assign-target-not-admin: '" + '<red><white>%admin%</white> does not have the admin permission (reportdesk.admin).' + "'"
    "  transferred: '" + '<green>Transferred report <white>#%id%</white> from <white>%from%</white> to <white>%staff%</white>.' + "'"
    "  transfer-not-owner: '" + '<red>Only the current handler <white>%staff%</white> can transfer this report, or use reportdesk.admin.close to override.' + "'"
    "  stats-per-staff: '" + '<gray>Per staff: <white>%count%</white> closed reports per handler (<white>%staff%</white> active handlers)' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <admin> <dark_gray>- <gray>Assign a report to an online admin' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <admin> <dark_gray>- <gray>Transfer a claimed report to another admin' + "'"
  )
  zh_CN = @(
    "  assigned: '" + '<green>已将举报 <white>#%id%</white> 指派给 <white>%staff%</white>。' + "'"
    "  already-assigned: '" + '<yellow>举报 <white>#%id%</white> 已由 <white>%staff%</white> 处理中，无需重复指派。' + "'"
    "  assign-target-offline: '" + '<red>管理员 <white>%admin%</white> 不在线，无法指派。' + "'"
    "  assign-target-not-admin: '" + '<red><white>%admin%</white> 没有管理权限（reportdesk.admin）。' + "'"
    "  transferred: '" + '<green>已将举报 <white>#%id%</white> 从 <white>%from%</white> 转派给 <white>%staff%</white>。' + "'"
    "  transfer-not-owner: '" + '<red>只有当前处理人 <white>%staff%</white> 可转派此举报，或需 reportdesk.admin.close 权限覆盖。' + "'"
    "  stats-per-staff: '" + '<gray>人均处理量: <white>%count%</white> 条/人（共 <white>%staff%</white> 名处理员）' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <管理员> <dark_gray>- <gray>将举报指派给在线管理员' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <管理员> <dark_gray>- <gray>将已认领的举报转派给其他管理员' + "'"
  )
  zh_TW = @(
    "  assigned: '" + '<green>已將投訴 <white>#%id%</white> 指派給 <white>%staff%</white>。' + "'"
    "  already-assigned: '" + '<yellow>投訴 <white>#%id%</white> 已由 <white>%staff%</white> 處理中，無需重複指派。' + "'"
    "  assign-target-offline: '" + '<red>管理員 <white>%admin%</white> 不線上，無法指派。' + "'"
    "  assign-target-not-admin: '" + '<red><white>%admin%</white> 沒有管理權限（reportdesk.admin）。' + "'"
    "  transferred: '" + '<green>已將投訴 <white>#%id%</white> 從 <white>%from%</white> 轉派給 <white>%staff%</white>。' + "'"
    "  transfer-not-owner: '" + '<red>只有目前處理人 <white>%staff%</white> 可轉派此投訴，或需 reportdesk.admin.close 權限覆蓋。' + "'"
    "  stats-per-staff: '" + '<gray>人均處理量: <white>%count%</white> 件/人（共 <white>%staff%</white> 名處理員）' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <管理員> <dark_gray>- <gray>將投訴指派給線上管理員' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <管理員> <dark_gray>- <gray>將已認領的投訴轉派給其他管理員' + "'"
  )
  ja_JP = @(
    "  assigned: '" + '<green>通報 <white>#%id%</white> を <white>%staff%</white> に割り当てました。' + "'"
    "  already-assigned: '" + '<yellow>通報 <white>#%id%</white> はすでに <white>%staff%</white> が対応中なので変更はありません。' + "'"
    "  assign-target-offline: '" + '<red>管理者 <white>%admin%</white> がオフラインのため割り当てられません。' + "'"
    "  assign-target-not-admin: '" + '<red><white>%admin%</white> には管理者権限（reportdesk.admin）がありません。' + "'"
    "  transferred: '" + '<green>通報 <white>#%id%</white> を <white>%from%</white> から <white>%staff%</white> に移管しました。' + "'"
    "  transfer-not-owner: '" + '<red>現在対応中の <white>%staff%</white> だけが移管できます（reportdesk.admin.close 権限で上書き可）。' + "'"
    "  stats-per-staff: '" + '<gray>担当あたり処理件数: <white>%count%</white> 件/人（担当者 <white>%staff%</white> 名）' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <管理者> <dark_gray>- <gray>オンラインの管理者に通報を割り当てる' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <管理者> <dark_gray>- <gray>引き受けた通報を他の管理者に移管する' + "'"
  )
  ko_KR = @(
    "  assigned: '" + '<green>제보 <white>#%id%</white>을(를) <white>%staff%</white>에게 배정했습니다.' + "'"
    "  already-assigned: '" + '<yellow>제보 <white>#%id%</white>은(는) 이미 <white>%staff%</white>가 처리 중이라 변경된 것이 없습니다.' + "'"
    "  assign-target-offline: '" + '<red>관리자 <white>%admin%</white>가 오프라인이라 배정할 수 없습니다.' + "'"
    "  assign-target-not-admin: '" + '<red><white>%admin%</white>에게 관리자 권한(reportdesk.admin)이 없습니다.' + "'"
    "  transferred: '" + '<green>제보 <white>#%id%</white>을(를) <white>%from%</white>에서 <white>%staff%</white>(으)로 이관했습니다.' + "'"
    "  transfer-not-owner: '" + '<red>현재 담당자 <white>%staff%</white>만이 이관을 할 수 있습니다(reportdesk.admin.close 권한으로 우회 가능).' + "'"
    "  stats-per-staff: '" + '<gray>1인당 처리량: <white>%count%</white>건/명 (담당 <white>%staff%</white>명)' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <관리자> <dark_gray>- <gray>온라인 관리자에게 제보를 배정합니다' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <관리자> <dark_gray>- <gray>담당 중인 제보를 다른 관리자에게 이관합니다' + "'"
  )
  de_DE = @(
    "  assigned: '" + '<green>Meldung <white>#%id%</white> an <white>%staff%</white> zugewiesen.' + "'"
    "  already-assigned: '" + '<yellow>Meldung <white>#%id%</white> wird bereits von <white>%staff%</white> bearbeitet; nichts geändert.' + "'"
    "  assign-target-offline: '" + '<red>Admin <white>%admin%</white> ist offline. Der Ziel-Admin muss online sein.' + "'"
    "  assign-target-not-admin: '" + '<red><white>%admin%</white> hat keine Admin-Berechtigung (reportdesk.admin).' + "'"
    "  transferred: '" + '<green>Meldung <white>#%id%</white> von <white>%from%</white> an <white>%staff%</white> übergeben.' + "'"
    "  transfer-not-owner: '" + '<red>Nur der aktuelle Bearbeiter <white>%staff%</white> kann diese Meldung übergeben (oder mit reportdesk.admin.close übersteuern).' + "'"
    "  stats-per-staff: '" + '<gray>Pro Kopf: <white>%count%</white> erledigte Meldungen pro Bearbeiter (<white>%staff%</white> aktiv)' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <admin> <dark_gray>- <gray>Meldung an einen online-Admin zuweisen' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <admin> <dark_gray>- <gray>Übertragene Meldung an einen anderen Admin übergeben' + "'"
  )
  fr_FR = @(
    "  assigned: '" + '<green>Rapport <white>#%id%</white> assigné à <white>%staff%</white>.' + "'"
    "  already-assigned: '" + '<yellow>Le rapport <white>#%id%</white> est déjà géré par <white>%staff%</white> ; rien n'a changé.' + "'"
    "  assign-target-offline: '" + '<red>L’administrateur <white>%admin%</white> est hors ligne. La cible de l’assignation doit être en ligne.' + "'"
    "  assign-target-not-admin: '" + '<red><white>%admin%</white> n’a pas la permission d’administrateur (reportdesk.admin).' + "'"
    "  transferred: '" + '<green>Rapport <white>#%id%</white> transféré de <white>%from%</white> à <white>%staff%</white>.' + "'"
    "  transfer-not-owner: '" + '<red>Seul le gestionnaire actuel <white>%staff%</white> peut transférer ce rapport (ou forcer avec reportdesk.admin.close).' + "'"
    "  stats-per-staff: '" + '<gray>Par agent : <white>%count%</white> rapports clôturés par personne (<white>%staff%</white> agents actifs)' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <admin> <dark_gray>- <gray>Assigner un rapport à un administrateur en ligne' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <admin> <dark_gray>- <gray>Transférer un rapport pris en charge à un autre administrateur' + "'"
  )
  es_ES = @(
    "  assigned: '" + '<green>Reporte <white>#%id%</white> asignado a <white>%staff%</white>.' + "'"
    "  already-assigned: '" + '<yellow>El reporte <white>#%id%</white> ya lo está gestionando <white>%staff%</white>; sin cambios.' + "'"
    "  assign-target-offline: '" + '<red>El administrador <white>%admin%</white> está desconectado. El objetivo de la asignación debe estar en línea.' + "'"
    "  assign-target-not-admin: '" + '<red><white>%admin%</white> no tiene permisos de administrador (reportdesk.admin).' + "'"
    "  transferred: '" + '<green>Reporte <white>#%id%</white> transferido de <white>%from%</white> a <white>%staff%</white>.' + "'"
    "  transfer-not-owner: '" + '<red>Solo el responsable actual <white>%staff%</white> puede transferir este reporte (o forzar con reportdesk.admin.close).' + "'"
    "  stats-per-staff: '" + '<gray>Por persona: <white>%count%</white> reportes cerrados por responsable (<white>%staff%</white> activos)' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <admin> <dark_gray>- <gray>Asignar un reporte a un administrador en línea' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <admin> <dark_gray>- <gray>Transferir un reporte reclamado a otro administrador' + "'"
  )
  ru_RU = @(
    "  assigned: '" + '<green>Жалоба <white>#%id%</white> назначена <white>%staff%</white>.' + "'"
    "  already-assigned: '" + '<yellow>Жалобу <white>#%id%</white> уже разбирает <white>%staff%</white>; изменений нет.' + "'"
    "  assign-target-offline: '" + '<red>Администратор <white>%admin%</white> не в сети. Целевой администратор должен быть онлайн.' + "'"
    "  assign-target-not-admin: '" + '<red>У <white>%admin%</white> нет прав администратора (reportdesk.admin).' + "'"
    "  transferred: '" + '<green>Жалоба <white>#%id%</white> передана от <white>%from%</white> к <white>%staff%</white>.' + "'"
    "  transfer-not-owner: '" + '<red>Передать жалобу может только текущий исполнитель <white>%staff%</white> (или принудительно с правом reportdesk.admin.close).' + "'"
    "  stats-per-staff: '" + '<gray>На человека: <white>%count%</white> закрытых жалоб на исполнителя (<white>%staff%</white> исполнителей)' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <admin> <dark_gray>- <gray>Назначить жалобу онлайн-администратору' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <admin> <dark_gray>- <gray>Передать взятую в работу жалобу другому администратору' + "'"
  )
  tr_TR = @(
    "  assigned: '" + '<white>#%id%</white> şikayeti <white>%staff%</white> kişisine atandı.' + "'"
    "  already-assigned: '" + '<yellow><white>#%id%</white> şikayeti zaten <white>%staff%</white> tarafından işleniyor; değişiklik yok.' + "'"
    "  assign-target-offline: '" + '<red>Yönetici <white>%admin%</white> çevrimdışı. Atama hedefi çevrimiçi olmalıdır.' + "'"
    "  assign-target-not-admin: '" + '<red><white>%admin%</white> yetkisi yok (reportdesk.admin).' + "'"
    "  transferred: '" + '<white>#%id%</white> şikayeti <white>%from%</white> üzerinden <white>%staff%</white> kişisine devredildi.' + "'"
    "  transfer-not-owner: '" + '<red>Bu şikayeti yalnızca mevcut sorumlu <white>%staff%</white> devredebilir (veya reportdesk.admin.close ile zorlayabilir).' + "'"
    "  stats-per-staff: '" + '<gray>Kişi başı: <white>%count%</white> kapanan şikayet/yönetici (<white>%staff%</white> sorumlu)' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <yönetici> <dark_gray>- <gray>Şikayeti çevrimiçi bir yöneticiye atar' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <yönetici> <dark_gray>- <gray>Devralınan şikayeti başka bir yöneticiye devreder' + "'"
  )
  pl_PL = @(
    "  assigned: '" + '<green>Zgłoszenie <white>#%id%</white> przydzielone do <white>%staff%</white>.' + "'"
    "  already-assigned: '" + '<yellow>Zgłoszenie <white>#%id%</white> jest już obsługiwane przez <white>%staff%</white>; bez zmian.' + "'"
    "  assign-target-offline: '" + '<red>Administrator <white>%admin%</white> jest offline. Cel przydziału musi być online.' + "'"
    "  assign-target-not-admin: '" + '<red><white>%admin%</white> nie ma uprawnień administratora (reportdesk.admin).' + "'"
    "  transferred: '" + '<green>Zgłoszenie <white>#%id%</white> przekazane od <white>%from%</white> do <white>%staff%</white>.' + "'"
    "  transfer-not-owner: '" + '<red>Tylko aktualny opiekun <white>%staff%</white> może przekazać to zgłoszenie (lub wymusić przez reportdesk.admin.close).' + "'"
    "  stats-per-staff: '" + '<gray>Na osobę: <white>%count%</white> zamkniętych zgłoszeń na administratora (<white>%staff%</white> aktywnych)' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <admin> <dark_gray>- <gray>Przydziel zgłoszenie administratorowi online' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <admin> <dark_gray>- <gray>Przekazane zgłoszenie do innego administratora' + "'"
  )
  pt_BR = @(
    "  assigned: '" + '<green>Denúncia <white>#%id%</white> atribuída a <white>%staff%</white>.' + "'"
    "  already-assigned: '" + '<yellow>A denúncia <white>#%id%</white> já está com <white>%staff%</white>; nada mudou.' + "'"
    "  assign-target-offline: '" + '<red>O administrador <white>%admin%</white> está offline. O alvo da atribuição precisa estar online.' + "'"
    "  assign-target-not-admin: '" + '<red><white>%admin%</white> não tem permissão de administrador (reportdesk.admin).' + "'"
    "  transferred: '" + '<green>Denúncia <white>#%id%</white> transferida de <white>%from%</white> para <white>%staff%</white>.' + "'"
    "  transfer-not-owner: '" + '<red>Somente o responsável atual <white>%staff%</white> pode transferir esta denúncia (ou force com reportdesk.admin.close).' + "'"
    "  stats-per-staff: '" + '<gray>Por pessoa: <white>%count%</white> denúncias encerradas por responsável (<white>%staff%</white> ativos)' + "'"
    "  help-assign: '" + '<gray>/reports assign <id> <admin> <dark_gray>- <gray>Atribuir uma denúncia a um admin online' + "'"
    "  help-transfer: '" + '<gray>/reports transfer <id> <admin> <dark_gray>- <gray>Transferir uma denúncia assumida para outro admin' + "'"
  )
}

$notifyKeys = @{
  en_US = @(
    "  staff-assigned: '" + '<gold>Report <white>#%id%</white> <gray>was assigned to <white>%staff%</white>.' + "'"
    "  staff-transferred: '" + '<gold>Report <white>#%id%</white> <gray>was transferred to <white>%staff%</white>.' + "'"
  )
  zh_CN = @(
    "  staff-assigned: '" + '<gold>举报 <white>#%id%</white> <gray>已指派给 <white>%staff%</white>。' + "'"
    "  staff-transferred: '" + '<gold>举报 <white>#%id%</white> <gray>已转派给 <white>%staff%</white>。' + "'"
  )
  zh_TW = @(
    "  staff-assigned: '" + '<gold>投訴 <white>#%id%</white> <gray>已指派給 <white>%staff%</white>。' + "'"
    "  staff-transferred: '" + '<gold>投訴 <white>#%id%</white> <gray>已轉派給 <white>%staff%</white>。' + "'"
  )
  ja_JP = @(
    "  staff-assigned: '" + '<gold>通報 <white>#%id%</white> が <white>%staff%</white> に割り当てられました。' + "'"
    "  staff-transferred: '" + '<gold>通報 <white>#%id%</white> が <white>%staff%</white> に移管されました。' + "'"
  )
  ko_KR = @(
    "  staff-assigned: '" + '<gold>제보 <white>#%id%</white>이(가) <white>%staff%</white>에게 배정되었습니다.' + "'"
    "  staff-transferred: '" + '<gold>제보 <white>#%id%</white>이(가) <white>%staff%</white>에게 이관되었습니다.' + "'"
  )
  de_DE = @(
    "  staff-assigned: '" + '<gold>Meldung <white>#%id%</white> wurde an <white>%staff%</white> zugewiesen.' + "'"
    "  staff-transferred: '" + '<gold>Meldung <white>#%id%</white> wurde an <white>%staff%</white> übergeben.' + "'"
  )
  fr_FR = @(
    "  staff-assigned: '" + '<gold>Le rapport <white>#%id%</white> a été assigné à <white>%staff%</white>.' + "'"
    "  staff-transferred: '" + '<gold>Le rapport <white>#%id%</white> a été transféré à <white>%staff%</white>.' + "'"
  )
  es_ES = @(
    "  staff-assigned: '" + '<gold>El reporte <white>#%id%</white> fue asignado a <white>%staff%</white>.' + "'"
    "  staff-transferred: '" + '<gold>El reporte <white>#%id%</white> fue transferido a <white>%staff%</white>.' + "'"
  )
  ru_RU = @(
    "  staff-assigned: '" + '<gold>Жалоба <white>#%id%</white> назначена <white>%staff%</white>.' + "'"
    "  staff-transferred: '" + '<gold>Жалоба <white>#%id%</white> передана <white>%staff%</white>.' + "'"
  )
  tr_TR = @(
    "  staff-assigned: '" + '<gold><white>#%id%</white> şikayeti <white>%staff%</white> kişisine atandı.' + "'"
    "  staff-transferred: '" + '<gold><white>#%id%</white> şikayeti <white>%staff%</white> kişisine devredildi.' + "'"
  )
  pl_PL = @(
    "  staff-assigned: '" + '<gold>Zgłoszenie <white>#%id%</white> przydzielone do <white>%staff%</white>.' + "'"
    "  staff-transferred: '" + '<gold>Zgłoszenie <white>#%id%</white> przekazane do <white>%staff%</white>.' + "'"
  )
  pt_BR = @(
    "  staff-assigned: '" + '<gold>A denúncia <white>#%id%</white> foi atribuída a <white>%staff%</white>.' + "'"
    "  staff-transferred: '" + '<gold>A denúncia <white>#%id%</white> foi transferida para <white>%staff%</white>.' + "'"
  )
}

$guiKeys = @{
  en_US = @(
    "    stats-per-staff-name: '" + '<aqua>Per-Capita Handling' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>Closed per active handler: <white>%per-staff%</white>' + "'"
  )
  zh_CN = @(
    "    stats-per-staff-name: '" + '<aqua>人均处理量' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>每名处理员平均结案: <white>%per-staff%</white>' + "'"
  )
  zh_TW = @(
    "    stats-per-staff-name: '" + '<aqua>人均處理量' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>每名處理員平均結案: <white>%per-staff%</white>' + "'"
  )
  ja_JP = @(
    "    stats-per-staff-name: '" + '<aqua>担当あたり処理件数' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>担当1人あたり平均閉鎖件数: <white>%per-staff%</white>' + "'"
  )
  ko_KR = @(
    "    stats-per-staff-name: '" + '<aqua>1인당 처리량' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>담당자 1인당 평균 종료 건수: <white>%per-staff%</white>' + "'"
  )
  de_DE = @(
    "    stats-per-staff-name: '" + '<aqua>Pro-Kopf-Bearbeitung' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>Ø erledigte Meldungen pro Bearbeiter: <white>%per-staff%</white>' + "'"
  )
  fr_FR = @(
    "    stats-per-staff-name: '" + '<aqua>Chiffre par agent' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>Rapports clôturés par agent : <white>%per-staff%</white>' + "'"
  )
  es_ES = @(
    "    stats-per-staff-name: '" + '<aqua>Trabajo por persona' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>Reportes cerrados por responsable: <white>%per-staff%</white>' + "'"
  )
  ru_RU = @(
    "    stats-per-staff-name: '" + '<aqua>Нагрузка на одного' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>Закрытых жалоб на исполнителя: <white>%per-staff%</white>' + "'"
  )
  tr_TR = @(
    "    stats-per-staff-name: '" + '<aqua>Kişi Başı İşlem' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>Kapanan şikayet/yönetici: <white>%per-staff%</white>' + "'"
  )
  pl_PL = @(
    "    stats-per-staff-name: '" + '<aqua>Praca na osobę' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>Zamkniętych zgłoszeń na administratora: <white>%per-staff%</white>' + "'"
  )
  pt_BR = @(
    "    stats-per-staff-name: '" + '<aqua>Trabalho por pessoa' + "'"
    "    stats-per-staff-lore:"
    "      - '" + '<gray>Denúncias encerradas por responsável: <white>%per-staff%</white>' + "'"
  )
}

$labelKeys = @{
  en_US = "        assign-notify-enabled: '" + 'Notify Assigned Admin' + "'"
  zh_CN = "        assign-notify-enabled: '" + '提醒被指派的管理员' + "'"
  zh_TW = "        assign-notify-enabled: '" + '提醒被指派的管理員' + "'"
  ja_JP = "        assign-notify-enabled: '" + '割り当て先管理者に通知' + "'"
  ko_KR = "        assign-notify-enabled: '" + '배정받은 관리자 알림' + "'"
  de_DE = "        assign-notify-enabled: '" + 'Zugewiesenen Admin Benachrichtigen' + "'"
  fr_FR = "        assign-notify-enabled: '" + 'Notifier L’administrateur Assigné' + "'"
  es_ES = "        assign-notify-enabled: '" + 'Notificar al Administrador Asignado' + "'"
  ru_RU = "        assign-notify-enabled: '" + 'Уведомлять Назначенного Администратора' + "'"
  tr_TR = "        assign-notify-enabled: '" + 'Atanan Yöneticiye Bildirim' + "'"
  pl_PL = "        assign-notify-enabled: '" + 'Powiadamiaj Przydzielonego Administratora' + "'"
  pt_BR = "        assign-notify-enabled: '" + 'Avisar o Administrador Atribuído' + "'"
}

$descKeys = @{
  en_US = "        assign-notify-enabled: '" + 'Pings the admin who receives an assigned or transferred report (only when the staff broadcast is off; admins without the staff permission stay muted).' + "'"
  zh_CN = "        assign-notify-enabled: '" + '指派或转派举报时单独提醒被指派的管理员（仅在关闭 staff 广播时补发；未拥有 staff 权限的管理员保持静音）。' + "'"
  zh_TW = "        assign-notify-enabled: '" + '指派或轉派投訴時單獨提醒被指派的管理員（僅在關閉 staff 廣播時補發；未擁有 staff 權限的管理員保持靜音）。' + "'"
  ja_JP = "        assign-notify-enabled: '" + '割り当て/移管時に担当管理者へ個別通知を送ります（スタッフブロードキャストOFF時のみ追加で送信。スタッフ権限のない管理者は通知を受けません）。' + "'"
  ko_KR = "        assign-notify-enabled: '" + '제보 배정/이관 시 담당 관리자에게 개별 알림을 보냅니다(스태프 브로드캐스트가 꺼져 있을 때만 추가 발송. 스태프 권한이 없는 관리자는 알림을 받지 않습니다).' + "'"
  de_DE = "        assign-notify-enabled: '" + 'Sendet dem zugewiesenen Admin eine gezielte Benachrichtigung (nur wenn der Staff-Broadcast aus ist; Admins ohne Staff-Berechtigung bleiben stumm).' + "'"
  fr_FR = "        assign-notify-enabled: '" + 'Envoie une alerte ciblée à l’administrateur qui reçoit un rapport assigné ou transféré (uniquement lorsque le broadcast staff est désactivé ; les admins sans la permission staff restent muets).' + "'"
  es_ES = "        assign-notify-enabled: '" + 'Avisa al administrador que recibe un reporte asignado o transferido (solo cuando el broadcast de staff está desactivado; los admins sin el permiso de staff siguen en silencio).' + "'"
  ru_RU = "        assign-notify-enabled: '" + 'Отправить целевое уведомление администратору, получившему назначенную или переданную жалобу (только если групповые уведомления выключены; администраторы без права staff остаются без уведомлений).' + "'"
  tr_TR = "        assign-notify-enabled: '" + 'Şikayet atanırken/devredilirken hedef yöneticiye özel uyarı gönderir (yalnızca ekip yayını kapalıyken eklenir; staff yetkisi olmayan yöneticiler sessiz kalır).' + "'"
  pl_PL = "        assign-notify-enabled: '" + 'Wyślij powiadomienie do administratora, któremu przydzielono lub przekazano zgłoszenie (tylko gdy broadcast staff jest wyłączony; administratorzy bez uprawnień staff pozostają wyciszeni).' + "'"
  pt_BR = "        assign-notify-enabled: '" + 'Envia um aviso ao admin que recebe uma denúncia atribuída ou transferida (apenas quando o broadcast de staff está desligado; admins sem a permissão de staff ficam silenciosos).' + "'"
}

$ErrorActionPreference = 'Stop'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

foreach ($lang in @('en_US','zh_CN','zh_TW','ja_JP','ko_KR','de_DE','fr_FR','es_ES','ru_RU','tr_TR','pl_PL','pt_BR')) {
  $path = Join-Path $root "$lang.yml"
  $lines = [System.IO.File]::ReadAllLines($path)
  $out = New-Object System.Collections.Generic.List[string]
  $out.AddRange($lines)

  # 1. admin keys after sla-report-empty:
  $idx = -1
  for ($k = 0; $k -lt $out.Count; $k++) { if ($out[$k].Trim() -eq 'sla-report-empty:') { $idx = $k; break } }
  if ($idx -lt 0) { throw "no sla-report-empty in $lang" }
  $out.InsertRange($idx + 1, $adminKeys[$lang])

  # 2. notify keys after broadcast-new:
  $idx = -1
  for ($k = 0; $k -lt $out.Count; $k++) { if ($out[$k].Trim() -eq 'broadcast-new:' -and $out[$k].StartsWith('  ')) { $idx = $k; break } }
  if ($idx -lt 0) { throw "no broadcast-new in $lang" }
  $out.InsertRange($idx + 1, $notifyKeys[$lang])

  # 3. gui keys before stats-line:
  $idx = -1
  for ($k = 0; $k -lt $out.Count; $k++) { if ($out[$k].Trim() -eq 'stats-line:') { $idx = $k; break } }
  if ($idx -lt 0) { throw "no stats-line in $lang" }
  $out.InsertRange($idx, $guiKeys[$lang])

  # 4+5. config-label (1st bare 6-space 'notifications:') / config-desc (2nd) entries
  $found = 0
  $finalOut = New-Object System.Collections.Generic.List[string]
  foreach ($line in $out) {
    $finalOut.Add($line)
    if ($line -eq '      notifications:') {
      $found++
      if ($found -eq 1) { $finalOut.Add($labelKeys[$lang]) }
      if ($found -eq 2) { $finalOut.Add($descKeys[$lang]) }
    }
  }
  if ($found -ne 2) { throw "expected 2 bare notifications: lines in $lang, found $found" }

  [System.IO.File]::WriteAllLines($path, $finalOut, $utf8NoBom)
  Write-Host "$lang : OK"
}
