# P2#12: add reports.bulk-max to the config editor label/desc blocks in all 12 langs.
$root = 'src\main\resources\lang'

$label = @{
  en_US = "          bulk-max: 'Batch Operation Limit'"
  zh_CN = "          bulk-max: '批量操作上限'"
  zh_TW = "          bulk-max: '批量操作上限'"
  ja_JP = "          bulk-max: '一括操作上限'"
  ko_KR = "          bulk-max: '일괄 처리 한도'"
  de_DE = "          bulk-max: 'Grenze für Bulk-Operationen'"
  fr_FR = "          bulk-max: 'Limite des opérations groupées'"
  es_ES = "          bulk-max: 'Límite de operación por lotes'"
  ru_RU = "          bulk-max: 'Лимит пакетных операций'"
  tr_TR = "          bulk-max: 'Toplu İşlem Sınırı'"
  pl_PL = "          bulk-max: 'Limit operacji zbiorczych'"
  pt_BR = "          bulk-max: 'Limite de Operação em Lote'"
}

$desc = @{
  en_US = "          bulk-max: 'Maximum reports a single bulk action (close/export) may touch, guarding against mass mistakes.'"
  zh_CN = "          bulk-max: '单次批量操作（关闭/导出）最多能处理的举报数，防止批量误操作。'"
  zh_TW = "          bulk-max: '單次批量操作（關閉/匯出）最多能處理的投訴數，防止批量誤操作。'"
  ja_JP = "          bulk-max: '一度の一括操作（クローズ/エクスポート）が扱える最大通報数。誤った一括操作を防ぐ。'"
  ko_KR = "          bulk-max: '일괄 작업(종료/내보내기) 한 번에 처리할 수 있는 최대 제보 수. 대량 실수를 방지합니다.'"
  de_DE = "          bulk-max: 'Maximale Meldungen, die eine einzelne Bulk-Operation (Schließen/Export) betreffen darf; schützt vor massenhaften Fehlern.'"
  fr_FR = "          bulk-max: 'Nombre maximum de rapports qu’une opération groupée (fermeture/export) peut toucher, pour éviter les erreurs de masse.'"
  es_ES = "          bulk-max: 'Número máximo de reportes que una acción por lotes (cerrar/exportar) puede afectar; evita errores masivos.'"
  ru_RU = "          bulk-max: 'Максимум жалоб, на которые может повлиять одна пакетная операция (закрытие/экспорт); защита от массовых ошибок.'"
  tr_TR = "          bulk-max: 'Tek toplu işlem (kapatma/dışa aktarma)in dokunabileceği en fazla şikayet sayısı; toplu hataları önler.'"
  pl_PL = "          bulk-max: 'Maksymalna liczba zgłoszeń, które może dotknąć pojedyncza operacja zbiorcza (zamknięcie/eksport); chroni przed masowymi błędami.'"
  pt_BR = "          bulk-max: 'Número máximo de denúncias que uma ação em lote (fechar/exportar) pode afetar; evita erros em massa.'"
}

$ErrorActionPreference = 'Stop'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

foreach ($lang in @('en_US','zh_CN','zh_TW','ja_JP','ko_KR','de_DE','fr_FR','es_ES','ru_RU','tr_TR','pl_PL','pt_BR')) {
  $path = Join-Path $root "$lang.yml"
  $lines = [System.IO.File]::ReadAllLines($path)
  $out = New-Object System.Collections.Generic.List[string]
  $out.AddRange($lines)

  # config-label reports block: after the 'staff-stats-top:' label line (10sp)
  $idx = -1
  for ($k = 0; $k -lt $out.Count; $k++) {
    if ($out[$k].Trim().StartsWith('staff-stats-top:') -and $out[$k].StartsWith('          staff-stats-top:')) {
      $idx = $k; break
    }
  }
  if ($idx -lt 0) { throw "no staff-stats-top label in $lang" }
  $out.Insert($idx + 1, $label[$lang])

  # config-desc reports block: after the 'staff-stats-top:' desc line
  $idx = -1
  for ($k = 0; $k -lt $out.Count; $k++) {
    if ($out[$k].Trim().StartsWith('staff-stats-top:') -and $out[$k].StartsWith('          staff-stats-top:')) {
      $idx = $k
    }
  }
  # idx now points to the LAST staff-stats-top occurrence = the desc block
  if ($idx -lt 0) { throw "no staff-stats-top desc in $lang" }
  $out.Insert($idx + 1, $desc[$lang])

  [System.IO.File]::WriteAllLines($path, $out, $utf8NoBom)
  Write-Host "$lang : OK"
}
