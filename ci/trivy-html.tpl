<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="utf-8">
<title>Rapport de sécurité Trivy</title>
<style>
  body { font-family: Arial, sans-serif; margin: 24px; background: #f5f6f8; color: #222; }
  h1 { margin-bottom: 4px; }
  h2 { margin-top: 0; }
  .box { background: #fff; border-radius: 8px; padding: 16px 20px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,.15); }
  table { border-collapse: collapse; width: 100%; font-size: 14px; }
  th { background: #2c3e50; color: #fff; text-align: left; padding: 8px; }
  td { padding: 8px; border-bottom: 1px solid #e3e6ea; vertical-align: top; }
  tr:hover td { background: #fafbfc; }
  .sev { padding: 2px 8px; border-radius: 10px; color: #fff; font-weight: bold; font-size: 12px; }
  .CRITICAL { background: #c0392b; }
  .HIGH { background: #e67e22; }
  .MEDIUM { background: #f1c40f; color: #222; }
  .LOW { background: #3498db; }
  .UNKNOWN { background: #7f8c8d; }
  .ok { color: #1e8449; font-weight: bold; }
  .nofix { color: #c0392b; font-weight: bold; }
  code { background: #eef0f3; padding: 1px 5px; border-radius: 4px; }
</style>
</head>
<body>
<h1>Rapport de sécurité Trivy</h1>

<div class="box">
  <h2>Service : __SERVICE__</h2>
  <p>Image : <code>__IMAGE__</code></p>
  <p>Build Jenkins n°__BUILD__ - généré le __DATE__</p>
  <p>Seules les failles <b>HIGH</b> et <b>CRITICAL</b> avec un correctif disponible sont listées.
     Si aucun tableau n'apparaît plus bas, aucune faille de ce type n'a été trouvée.</p>
</div>

<div class="box">
  <h2>Comment lire ce rapport</h2>
  <table>
    <tr><th>Colonne</th><th>Signification</th></tr>
    <tr><td>Bibliothèque</td><td>Le composant concerné : un fichier <code>.jar</code> Java ou un paquet du système.</td></tr>
    <tr><td>CVE</td><td>Numéro officiel de la faille. Clique dessus pour lire le détail.</td></tr>
    <tr><td>Gravité</td><td><span class="sev CRITICAL">CRITICAL</span> bloque le pipeline.
        <span class="sev HIGH">HIGH</span> est à corriger bientôt mais ne bloque pas.</td></tr>
    <tr><td>Version installée</td><td>La version utilisée actuellement dans ton image.</td></tr>
    <tr><td>Version corrigée</td><td>La version qui contient le correctif. Il suffit de passer à cette version (ou plus récente).</td></tr>
    <tr><td>Description</td><td>Résumé du risque en une phrase.</td></tr>
  </table>
</div>

{{- range . }}
{{- if .Vulnerabilities }}
<div class="box">
  <h2>{{ escapeXML .Target }}</h2>
  <p>{{ len .Vulnerabilities }} faille(s) détectée(s)</p>
  <table>
    <tr>
      <th>Bibliothèque</th>
      <th>CVE</th>
      <th>Gravité</th>
      <th>Version installée</th>
      <th>Version corrigée</th>
      <th>Description</th>
    </tr>
    {{- range .Vulnerabilities }}
    <tr>
      <td>{{ escapeXML .PkgName }}</td>
      <td><a href="{{ .PrimaryURL }}">{{ escapeXML .VulnerabilityID }}</a></td>
      <td><span class="sev {{ .Severity }}">{{ .Severity }}</span></td>
      <td>{{ escapeXML .InstalledVersion }}</td>
      <td>{{ if .FixedVersion }}<span class="ok">{{ escapeXML .FixedVersion }}</span>{{ else }}<span class="nofix">Pas de correctif</span>{{ end }}</td>
      <td>{{ escapeXML .Title }}</td>
    </tr>
    {{- end }}
  </table>
</div>
{{- end }}
{{- end }}
</body>
</html>
