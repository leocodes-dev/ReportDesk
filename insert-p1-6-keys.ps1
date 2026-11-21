# P1#6 language insertions:
#   1. admin.* smart-review keys + help-smart  -> after the 'help-transfer:' line (2sp)
#   2. gui.filter-sort-smart                  -> after the 'filter-sort-priority:' line (2sp)
#   3. misc-data-lore garbage-rate line       -> before the 'misc-retention-name:' line (6sp item)
$root = 'src\main\resources\lang'

$smartKeys = @{
  en_US = @(
    "  smart-disabled: '" + '<gray>The smart priority layer is off (smart-priority.enabled=false); nothing to review.' + "'"
    "  smart-review-header: '" + '<gold>===== Smart Priority Review (%count% scored closed reports) =====' + "'"
    "  smart-review-invalid-days: '" + '<red>Invalid days value: %days% (use 0-36500, 0 = all history).</red>' + "'"
    "  smart-review-empty: '" + '<gray>No scored closed reports in the window.' + "'"
    "  smart-review-scores: '" + '<gray>Scored <white>%scored%</white> <dark_gray>| <gray>Flagged <white>%flagged%</white> <dark_gray>| <gray>Actually garbage <white>%garbage%</white> <dark_gray>| <gray>Both <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>Flag precision: <white>%value%</white> (share of flagged reports that were actually garbage)' + "'"
    "  smart-review-recall: '" + '<gray>Garbage recall: <white>%value%</white> (share of garbage reports that were flagged)' + "'"
    "  smart-review-flag-rate: '" + '<gray>Garbage flag rate: <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [days] <dark_gray>- <gray>Review smart-priority precision/recall' + "'"
  )
  zh_CN = @(
    "  smart-disabled: '" + '<gray>智能优先级未开启（smart-priority.enabled=false），没有可复盘的数据。' + "'"
    "  smart-review-header: '" + '<gold>===== 智能优先级复盘（%count% 条已评分结案）=====' + "'"
    "  smart-review-invalid-days: '" + '<red>非法天数: %days%（使用 0-36500，0 表示全部）。</red>' + "'"
    "  smart-review-empty: '" + '<gray>窗口内没有已评分的结案举报。' + "'"
    "  smart-review-scores: '" + '<gray>已评分 <white>%scored%</white> <dark_gray>| <gray>已标记 <white>%flagged%</white> <dark_gray>| <gray>实为垃圾 <white>%garbage%</white> <dark_gray>| <gray>两者皆是 <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>标记精确率: <white>%value%</white>（被标记中实为垃圾占比）' + "'"
    "  smart-review-recall: '" + '<gray>垃圾召回率: <white>%value%</white>（真垃圾中被标记占比）' + "'"
    "  smart-review-flag-rate: '" + '<gray>垃圾标记率: <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [天数] <dark_gray>- <gray>智能优先级复盘（精确率/召回率/分布）' + "'"
  )
  zh_TW = @(
    "  smart-disabled: '" + '<gray>智慧優先級未開啟（smart-priority.enabled=false），沒有可複盤的數據。' + "'"
    "  smart-review-header: '" + '<gold>===== 智慧優先級複盤（%count% 條已評分結案）=====' + "'"
    "  smart-review-invalid-days: '" + '<red>非法天數: %days%（使用 0-36500，0 表示全部）。</red>' + "'"
    "  smart-review-empty: '" + '<gray>窗口內沒有已評分的結案投訴。' + "'"
    "  smart-review-scores: '" + '<gray>已評分 <white>%scored%</white> <dark_gray>| <gray>已標記 <white>%flagged%</white> <dark_gray>| <gray>實為垃圾 <white>%garbage%</white> <dark_gray>| <gray>兩者皆是 <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>標記精確率: <white>%value%</white>（被標記中實為垃圾佔比）' + "'"
    "  smart-review-recall: '" + '<gray>垃圾召回率: <white>%value%</white>（真垃圾中被標記佔比）' + "'"
    "  smart-review-flag-rate: '" + '<gray>垃圾標記率: <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [天數] <dark_gray>- <gray>智慧優先級複盤（精確率/召回率/分布）' + "'"
  )
  ja_JP = @(
    "  smart-disabled: '" + '<gray>スマート優先度は無効です（smart-priority.enabled=false）。レビュー可能なデータがありません。' + "'"
    "  smart-review-header: '" + '<gold>===== スマート優先度レビュー（評価済みクローズ %count% 件）=====' + "'"
    "  smart-review-invalid-days: '" + '<red>無効な日数: %days%（0-36500 を使用、0 はすべて）。</red>' + "'"
    "  smart-review-empty: '" + '<gray>対象期間内に評価済みのクローズ通報はありません。' + "'"
    "  smart-review-scores: '" + '<gray>評価済み <white>%scored%</white> <dark_gray>| <gray>フラグ <white>%flagged%</white> <dark_gray>| <gray>実際は迷惑 <white>%garbage%</white> <dark_gray>| <gray>両方 <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>フラグの適合率: <white>%value%</white>（フラグ付きのうち実際に迷惑の割合）' + "'"
    "  smart-review-recall: '" + '<gray>迷惑の召回率: <white>%value%</white>（実際の迷惑のうちフラグされた割合）' + "'"
    "  smart-review-flag-rate: '" + '<gray>迷惑フラグ率: <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [日数] <dark_gray>- <gray>スマート優先度の精度/召回率をレビュー' + "'"
  )
  ko_KR = @(
    "  smart-disabled: '" + '<gray>스마트 우선순위가 꺼져 있습니다(smart-priority.enabled=false). 검토할 데이터가 없습니다.' + "'"
    "  smart-review-header: '" + '<gold>===== 스마트 우선순위 검토 (평가된 종료 건 %count%건) =====' + "'"
    "  smart-review-invalid-days: '" + '<red>잘못된 일수: %days% (0-36500 사용, 0은 전체).</red>' + "'"
    "  smart-review-empty: '" + '<gray>해당 기간에 평가된 종료 제보가 없습니다.' + "'"
    "  smart-review-scores: '" + '<gray>평가됨 <white>%scored%</white> <dark_gray>| <gray>표시됨 <white>%flagged%</white> <dark_gray>| <gray>실제 스팸 <white>%garbage%</white> <dark_gray>| <gray>양쪽 해당 <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>표시 정확률: <white>%value%</white> (표시된 것 중 실제 스팸 비율)' + "'"
    "  smart-review-recall: '" + '<gray>스팸 재현률: <white>%value%</white> (실제 스팸 중 표시된 비율)' + "'"
    "  smart-review-flag-rate: '" + '<gray>스팸 표시율: <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [일수] <dark_gray>- <gray>스마트 우선순위 정밀도/재현율 검토' + "'"
  )
  de_DE = @(
    "  smart-disabled: '" + '<gray>Intelligente Priorisierung ist deaktiviert (smart-priority.enabled=false); es gibt nichts zu überprüfen.' + "'"
    "  smart-review-header: '" + '<gold>===== Intelligente-Priorität-Auswertung (%count% bewertete abgeschlossene Meldungen) =====' + "'"
    "  smart-review-invalid-days: '" + '<red>Ungültige Tage: %days% (0-36500 verwenden, 0 = alle).</red>' + "'"
    "  smart-review-empty: '" + '<gray>Keine bewerteten abgeschlossenen Meldungen im Zeitraum.' + "'"
    "  smart-review-scores: '" + '<gray>Bewertet <white>%scored%</white> <dark_gray>| <gray>Markiert <white>%flagged%</white> <dark_gray>| <gray>Tatsächlich Spam <white>%garbage%</white> <dark_gray>| <gray>Beides <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>Präzision der Markierung: <white>%value%</white> (Anteil der Markierten, die tatsächlich Spam waren)' + "'"
    "  smart-review-recall: '" + '<gray>Erinnerlichkeit des Spam-Bereichs: <white>%value%</white> (Anteil des Spam, der markiert wurde)' + "'"
    "  smart-review-flag-rate: '" + '<gray>Spam-Markierungsrate: <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [tage] <dark_gray>- <gray>Präzision/Erinnerlichkeit der intelligenten Priorisierung prüfen' + "'"
  )
  fr_FR = @(
    '  smart-disabled: "' + "La priorité intelligente est désactivée (smart-priority.enabled=false) ; rien à examiner." + '"'
    "  smart-review-header: '" + '<gold>===== Revue de la priorité intelligente (%count% rapports clos évalués) =====' + "'"
    "  smart-review-invalid-days: '" + '<red>Jours invalides : %days% (utiliser 0-36500, 0 = tous).</red>' + "'"
    "  smart-review-empty: '" + '<gray>Aucun rapport clos évalué sur la période.' + "'"
    "  smart-review-scores: '" + '<gray>Évalués <white>%scored%</white> <dark_gray>| <gray>Marqués <white>%flagged%</white> <dark_gray>| <gray>Effectivement spam <white>%garbage%</white> <dark_gray>| <gray>Les deux <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>Précision du marquage : <white>%value%</white> (part des marqués réellement spam)' + "'"
    "  smart-review-recall: '" + '<gray>Rappel du spam : <white>%value%</white> (part du spam effectivement marqué)' + "'"
    "  smart-review-flag-rate: '" + '<gray>Taux de marquage spam : <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [jours] <dark_gray>- <gray>Revue de la précision/rappel de la priorité intelligente' + "'"
  )
  es_ES = @(
    "  smart-disabled: '" + '<gray>La prioridad inteligente está desactivada (smart-priority.enabled=false); no hay nada que revisar.' + "'"
    "  smart-review-header: '" + '<gold>===== Revisión de prioridad inteligente (%count% reportes cerrados evaluados) =====' + "'"
    "  smart-review-invalid-days: '" + '<red>Días inválidos: %days% (usa 0-36500, 0 = todos).</red>' + "'"
    "  smart-review-empty: '" + '<gray>No hay reportes cerrados evaluados en el periodo.' + "'"
    "  smart-review-scores: '" + '<gray>Evaluados <white>%scored%</white> <dark_gray>| <gray>Marcados <white>%flagged%</white> <dark_gray>| <gray>Realmente basura <white>%garbage%</white> <dark_gray>| <gray>Ambos <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>Precisión del marcado: <white>%value%</white> (proporción de los marcados que eran basura)' + "'"
    "  smart-review-recall: '" + '<gray>Recuperación de basura: <white>%value%</white> (proporción de la basura realmente marcada)' + "'"
    "  smart-review-flag-rate: '" + '<gray>Tasa de marcado de basura: <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [dias] <dark_gray>- <gray>Revisión de precisión/recuperación de la prioridad inteligente' + "'"
  )
  ru_RU = @(
    "  smart-disabled: '" + '<gray>Умный приоритет выключен (smart-priority.enabled=false); нечего просматривать.' + "'"
    "  smart-review-header: '" + '<gold>===== Обзор умного приоритета (%count% закрытых жалоб с оценкой) =====' + "'"
    "  smart-review-invalid-days: '" + '<red>Неверные дни: %days% (используй 0-36500, 0 = все).</red>' + "'"
    "  smart-review-empty: '" + '<gray>В окне нет закрытых жалоб с оценкой.' + "'"
    "  smart-review-scores: '" + '<gray>Оценено <white>%scored%</white> <dark_gray>| <gray>Помечено <white>%flagged%</white> <dark_gray>| <gray>Фактически мусор <white>%garbage%</white> <dark_gray>| <gray>Оба <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>Точность пометки: <white>%value%</white> (доля помеченных, оказавшихся мусором)' + "'"
    "  smart-review-recall: '" + '<gray>Полнота по мусору: <white>%value%</white> (доля мусора, попавшего в пометку)' + "'"
    "  smart-review-flag-rate: '" + '<gray>Доля помеченного мусора: <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [дни] <dark_gray>- <gray>Просмотр точности/полноты умного приоритета' + "'"
  )
  tr_TR = @(
    "  smart-disabled: '" + '<gray>Akıllı öncelik kapalı (smart-priority.enabled=false); incelencek veri yok.' + "'"
    "  smart-review-header: '" + '<gold>===== Akıllı Öncelik İncelemesi (%count% puanlı kapanan şikayet) =====' + "'"
    "  smart-review-invalid-days: '" + '<red>Geçersiz gün: %days% (0-36500 kullan, 0 = tümü).</red>' + "'"
    "  smart-review-empty: '" + '<gray>Süreçte puanlanmış kapanan şikayet yok.' + "'"
    "  smart-review-scores: '" + '<gray>Puanlı <white>%scored%</white> <dark_gray>| <gray>İşaretli <white>%flagged%</white> <dark_gray>| <gray>Asıl spam <white>%garbage%</white> <dark_gray>| <gray>Her ikisi <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>İşaret duyarlılığı: <white>%value%</white> (işaretlilerden asıl spam oranı)' + "'"
    "  smart-review-recall: '" + '<gray>Spam yakalama: <white>%value%</white> (asıl spamden işaretlenen oran)' + "'"
    "  smart-review-flag-rate: '" + '<gray>Spam işaret oranı: <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [gün] <dark_gray>- <gray>Akıllı öncelik duyarlılık/yakalama incelemesi' + "'"
  )
  pl_PL = @(
    "  smart-disabled: '" + '<gray>Inteligentna priorytet jest wyłączony (smart-priority.enabled=false); brak danych do przeglądu.' + "'"
    "  smart-review-header: '" + '<gold>===== Przegląd inteligentnego priorytetu (%count% ocenionych zamkniętych zgłoszeń) =====' + "'"
    "  smart-review-invalid-days: '" + '<red>Nieprawidłowa liczba dni: %days% (użyj 0-36500, 0 = wszystkie).</red>' + "'"
    "  smart-review-empty: '" + '<gray>Brak ocenionych zamkniętych zgłoszeń w oknie.' + "'"
    "  smart-review-scores: '" + '<gray>Ocenione <white>%scored%</white> <dark_gray>| <gray>Oznaczone <white>%flagged%</white> <dark_gray>| <gray>Faktycznie spam <white>%garbage%</white> <dark_gray>| <gray>Oba <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>Precyzja oznaczeń: <white>%value%</white> (udział oznaczonych, które były spamenem)' + "'"
    "  smart-review-recall: '" + '<gray>Czułość na spam: <white>%value%</white> (udział spamu, który został oznaczony)' + "'"
    "  smart-review-flag-rate: '" + '<gray>Wskaźnik oznaczania spamu: <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [dni] <dark_gray>- <gray>Przegląd precyzji/czułości inteligentnego priorytetu' + "'"
  )
  pt_BR = @(
    "  smart-disabled: '" + '<gray>A prioridade inteligente está desativada (smart-priority.enabled=false); não há nada para revisar.' + "'"
    "  smart-review-header: '" + '<gold>===== Revisão da Prioridade Inteligente (%count% denúncias encerradas pontuadas) =====' + "'"
    "  smart-review-invalid-days: '" + '<red>Dias inválidos: %days% (use 0-36500, 0 = todas).</red>' + "'"
    "  smart-review-empty: '" + '<gray>Não há denúncias encerradas pontuadas no período.' + "'"
    "  smart-review-scores: '" + '<gray>Pontuadas <white>%scored%</white> <dark_gray>| <gray>Marquadas <white>%flagged%</white> <dark_gray>| <gray>Realmente spam <white>%garbage%</white> <dark_gray>| <gray>Ambas <white>%flagged-garbage%</white>' + "'"
    "  smart-review-precision: '" + '<gray>Previsão das marcações: <white>%value%</white> (proporção das marcadas que eram spam)' + "'"
    "  smart-review-recall: '" + '<gray>Recall do spam: <white>%value%</white> (proporção do spam realmente marcado)' + "'"
    "  smart-review-flag-rate: '" + '<gray>Taxa de marcação de spam: <white>%value%</white>' + "'"
    "  smart-review-bucket: '" + '<gray>%range% <white>%bar% <dark_gray>(%count%)' + "'"
    "  help-smart: '" + '<gray>/reports smart review [dias] <dark_gray>- <gray>Revisar precisão/recall da prioridade inteligente' + "'"
  )
}

$sortKeys = @{
  en_US = @( "  filter-sort-smart: '" + '<aqua>By Smart Score' + "'" )
  zh_CN = @( "  filter-sort-smart: '" + '<aqua>按智能分' + "'" )
  zh_TW = @( "  filter-sort-smart: '" + '<aqua>依智慧分' + "'" )
  ja_JP = @( "  filter-sort-smart: '" + '<aqua>スマートスコア順' + "'" )
  ko_KR = @( "  filter-sort-smart: '" + '<aqua>스마트 점수순' + "'" )
  de_DE = @( "  filter-sort-smart: '" + '<aqua>Nach Smart-Score' + "'" )
  fr_FR = @( "  filter-sort-smart: '" + '<aqua>Tri par score intelligent' + "'" )
  es_ES = @( "  filter-sort-smart: '" + '<aqua>Por puntuación inteligente' + "'" )
  ru_RU = @( "  filter-sort-smart: '" + '<aqua>По умному баллу' + "'" )
  tr_TR = @( "  filter-sort-smart: '" + '<aqua>Akıllı puana göre' + "'" )
  pl_PL = @( "  filter-sort-smart: '" + '<aqua>Według smart punktu' + "'" )
  pt_BR = @( "  filter-sort-smart: '" + '<aqua>Por pontuação inteligente' + "'" )
}

$loreLines = @{
  en_US = @( "      - '" + '<gray>Garbage flag rate: <white>%garbage-flag-rate%</white>' + "'" )
  zh_CN = @( "      - '" + '<gray>垃圾标记率: <white>%garbage-flag-rate%</white>' + "'" )
  zh_TW = @( "      - '" + '<gray>垃圾標記率: <white>%garbage-flag-rate%</white>' + "'" )
  ja_JP = @( "      - '" + '<gray>迷惑フラグ率: <white>%garbage-flag-rate%</white>' + "'" )
  ko_KR = @( "      - '" + '<gray>스팸 표시율: <white>%garbage-flag-rate%</white>' + "'" )
  de_DE = @( "      - '" + '<gray>Spam-Markierungsrate: <white>%garbage-flag-rate%</white>' + "'" )
  fr_FR = @( "      - '" + '<gray>Taux de marquage spam : <white>%garbage-flag-rate%</white>' + "'" )
  es_ES = @( "      - '" + '<gray>Tasa de marcado de basura: <white>%garbage-flag-rate%</white>' + "'" )
  ru_RU = @( "      - '" + '<gray>Доля помеченного мусора: <white>%garbage-flag-rate%</white>' + "'" )
  tr_TR = @( "      - '" + '<gray>Spam işaret oranı: <white>%garbage-flag-rate%</white>' + "'" )
  pl_PL = @( "      - '" + '<gray>Wskaźnik oznaczania spamu: <white>%garbage-flag-rate%</white>' + "'" )
  pt_BR = @( "      - '" + '<gray>Taxa de marcação de spam: <white>%garbage-flag-rate%</white>' + "'" )
}

$ErrorActionPreference = 'Stop'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

foreach ($lang in @('en_US','zh_CN','zh_TW','ja_JP','ko_KR','de_DE','fr_FR','es_ES','ru_RU','tr_TR','pl_PL','pt_BR')) {
  $path = Join-Path $root "$lang.yml"
  $lines = [System.IO.File]::ReadAllLines($path)
  $out = New-Object System.Collections.Generic.List[string]
  $out.AddRange($lines)

  # 1. smart keys after help-transfer: (insert before so order stays grouped)
  $idx = -1
  for ($k = 0; $k -lt $out.Count; $k++) { if ($out[$k].Trim().StartsWith('help-transfer:')) { $idx = $k; break } }
  if ($idx -lt 0) { throw "no help-transfer in $lang" }
  $out.InsertRange($idx + 1, [string[]]($smartKeys[$lang]))

  # 2. filter-sort-smart after filter-sort-priority:
  $idx = -1
  for ($k = 0; $k -lt $out.Count; $k++) { if ($out[$k].Trim().StartsWith('filter-sort-priority:')) { $idx = $k; break } }
  if ($idx -lt 0) { throw "no filter-sort-priority in $lang" }
  $out.InsertRange($idx + 1, [string[]]($sortKeys[$lang]))

  # 3. lore line before misc-retention-name:
  $idx = -1
  for ($k = 0; $k -lt $out.Count; $k++) { if ($out[$k].Trim().StartsWith('misc-retention-name:')) { $idx = $k; break } }
  if ($idx -lt 0) { throw "no misc-retention-name in $lang" }
  $out.InsertRange($idx, [string[]]($loreLines[$lang]))

  [System.IO.File]::WriteAllLines($path, $out, $utf8NoBom)
  Write-Host "$lang : OK"
}
