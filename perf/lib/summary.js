// Writes each run's results to perf/results/: the raw k6 summary (JSON) and a Markdown table
// with the median and 95th percentile per scenario, ready to paste into the report. No remote
// helper library is imported, so a run never downloads code.
const ms = (v) => (v === undefined ? 'n/a' : v.toFixed(1));

/**
 * Builds handleSummary for a test.
 * @param {string} name file name prefix, e.g. "search-edge"
 * @param {string[]} scenarios scenario names to report
 * @param {string} title heading for the Markdown table
 * @param {string[]} counters extra counter metrics to list under the table
 */
export function summaryWriter(name, scenarios, title, counters = []) {
  return function handleSummary(data) {
    const target = __ENV.TARGET_LABEL || __ENV.BASE_URL || '';
    const rows = scenarios.map((s) => {
      const d = data.metrics[`http_req_duration{scenario:${s}}`];
      const r = data.metrics[`http_reqs{scenario:${s}}`];
      const values = (d && d.values) || {};
      // A scenario passes only if its latency and request-count thresholds both hold, so a run
      // that sent no requests (for example after a failed login) never reads as a pass.
      const verdicts = [d, r].flatMap((m) => (m && m.thresholds ? Object.values(m.thresholds) : []));
      const passed = verdicts.length ? verdicts.every((t) => t.ok) : undefined;
      return `| ${s} | ${r ? r.values.count : 'n/a'} | ${ms(values.med)} | ${ms(values['p(95)'])} | ${passed === undefined ? 'n/a' : passed ? 'pass' : 'FAIL'} |`;
    });
    const table = [
      `## ${title}`,
      '',
      `Target: ${target}  `,
      `Run: ${new Date().toISOString()}`,
      '',
      '| Scenario | Requests | Median (ms) | p95 (ms) | Threshold |',
      '|---|---|---|---|---|',
      ...rows,
      '',
      ...counters.map((c) => `- ${c}: ${data.metrics[c] ? data.metrics[c].values.count : 0}`),
      '',
    ].join('\n');
    return {
      stdout: table + '\n',
      [`/perf/results/${name}.json`]: JSON.stringify(data, null, 2),
      [`/perf/results/${name}.md`]: table,
    };
  };
}
