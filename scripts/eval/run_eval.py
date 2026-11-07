"""Automatic evaluation on the version chains (PRD 10.1).

Replays every chain of a set (problems/split.json) through the running application,
once per variant, then compares with the reference observations:

    python scripts/eval/run_eval.py --set tuning
    python scripts/eval/run_eval.py --set tuning --modes proposed --problems two-sum
    python scripts/eval/run_eval.py --reanalyse results/eval/<run-id>   # no new LLM calls

Needs the stack running (docker compose up) with ANTHROPIC_API_KEY set. Each run spends
LLM budget: about $0.0005 per submission in `proposed` and `baseline_b`.

Writes results/eval/<run-id>/:
    responses.jsonl          every API response, for re-analysis without re-running
    observations.csv         every proposed observation (confirmed and rejected) with its match
    reference_matches.csv    every reference observation and whether it was found
    baseline_b_review.csv    Baseline B answers next to the reference, for manual labelling
    hints_review.csv         shown hints, for the manual leak check
    summary.json / summary.md

The model cannot be run with a fixed temperature (current Claude models reject sampling
parameters), so reproducibility comes from keeping responses.jsonl and the exports.
"""
import argparse
import csv
import io
import json
import os
import sys
import time
import urllib.error
import urllib.request
from collections import Counter, defaultdict
from datetime import datetime, timezone

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SEVERITY_WEIGHT = {"high": 3, "medium": 2, "low": 1}
LINE_TOLERANCE = 1


def http(base, method, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base + path, data=data, method=method,
                                 headers={"Content-Type": "application/json"} if data else {})
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            raw = resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"{method} {path} -> {e.code}: {e.read().decode('utf-8', 'replace')[:300]}") from e
    if path.endswith(".csv"):
        return list(csv.DictReader(io.StringIO(raw)))
    return json.loads(raw) if raw else None


def read_json(*parts):
    with open(os.path.join(ROOT, *parts), encoding="utf-8") as f:
        return json.load(f)


def read_text(*parts):
    with open(os.path.join(ROOT, *parts), encoding="utf-8") as f:
        return f.read()


WRONG_ANSWER_TYPES = {"correctness", "edge_case"}


def matches(observation, reference, lenient=False):
    """Same type and a line within the reference lines (±1).

    Lenient: correctness and edge_case count as the same type, since the line between them is a
    judgement call (both mean a wrong answer); efficiency must still match exactly.
    """
    same_type = observation["type"] == reference["type"] or (
        lenient and {observation["type"], reference["type"]} <= WRONG_ANSWER_TYPES)
    if not same_type:
        return False
    if observation.get("line") is None:
        return reference["type"] == "efficiency"
    return any(abs(observation["line"] - line) <= LINE_TOLERANCE for line in reference["lines"])


def expected_classification(previous, current):
    prev, cur = set(previous["expectedFailures"]), set(current["expectedFailures"])
    return {"resolved": sorted(prev - cur), "regression": sorted(cur - prev), "remaining": sorted(prev & cur)}


def actual_classification(classification):
    return {k: sorted(t["testId"] for t in classification[k]) for k in ("resolved", "regression", "remaining")}


def run_chain(base, run_id, problem_id, mode, out):
    chain = read_json("problems", problem_id, "chain", "chain.json")
    session = http(base, "POST", "/api/sessions",
                   {"problemId": problem_id, "mode": mode, "participantId": f"eval-{run_id}"})
    results = []
    for v in chain["versions"]:
        code = read_text("problems", problem_id, "chain", v["file"])
        start = time.time()
        response = http(base, "POST", f"/api/sessions/{session['sessionId']}/submissions", {"code": code})
        seconds = round(time.time() - start, 2)
        record = {"runId": run_id, "problem": problem_id, "mode": mode, "version": v["version"],
                  "seconds": seconds, "response": response}
        out.write(json.dumps(record, ensure_ascii=False) + "\n")
        out.flush()
        results.append((v, record))
        shown = len(response["observations"])
        print(f"  {problem_id} {mode} v{v['version']}: {response['report']['summary']['passed']}/"
              f"{response['report']['summary']['total']} tests, llm={response['llmStatus']}, "
              f"{shown} shown, {seconds}s")
    return chain, results


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--set", choices=["tuning", "test"])
    parser.add_argument("--reanalyse", help="results directory to analyse again from its responses.jsonl")
    parser.add_argument("--modes", default="proposed,baseline_b,baseline_a")
    parser.add_argument("--problems", help="comma-separated subset of the set")
    parser.add_argument("--base-url", default=os.environ.get("REVIEWER_URL", "http://localhost:8080"))
    args = parser.parse_args()

    if args.reanalyse:
        reanalyse(args.reanalyse, args.base_url)
        return
    if not args.set:
        sys.exit("--set is required")
    split = read_json("problems", "split.json")
    problems = split[args.set]
    if args.problems:
        wanted = args.problems.split(",")
        problems = [p for p in problems if p in wanted]
    if not problems:
        sys.exit(f"No problems in set '{args.set}'")
    modes = args.modes.split(",")

    run_id = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    out_dir = os.path.join(ROOT, "results", "eval", f"{run_id}-{args.set}")
    os.makedirs(out_dir)
    print(f"run {run_id}: set={args.set} problems={problems} modes={modes}")

    runs = []
    with open(os.path.join(out_dir, "responses.jsonl"), "w", encoding="utf-8") as out:
        for problem_id in problems:
            for mode in modes:
                chain, results = run_chain(args.base_url, run_id, problem_id, mode, out)
                runs.append((problem_id, mode, chain, results))

    exported = {t: http(args.base_url, "GET", f"/api/export/{t}.csv") for t in ("observations", "llm_calls")}
    summary = analyse(runs, exported, out_dir)
    summary.update({"runId": run_id, "set": args.set, "problems": problems, "variants": modes})
    with open(os.path.join(out_dir, "summary.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2, ensure_ascii=False)
    with open(os.path.join(out_dir, "summary.md"), "w", encoding="utf-8") as f:
        f.write(markdown(summary))
    print(markdown(summary))
    print(f"results in {out_dir}")


def reanalyse(out_dir, base_url):
    """Rebuilds the summary from saved responses; no LLM calls."""
    grouped = defaultdict(list)
    meta = {}
    with open(os.path.join(out_dir, "responses.jsonl"), encoding="utf-8") as f:
        for line in f:
            record = json.loads(line)
            grouped[(record["problem"], record["mode"])].append(record)
            meta = {"runId": record["runId"]}
    runs = []
    for (problem_id, mode), records in grouped.items():
        chain = read_json("problems", problem_id, "chain", "chain.json")
        by_version = {v["version"]: v for v in chain["versions"]}
        runs.append((problem_id, mode, chain, [(by_version[r["version"]], r) for r in records]))
    exported = {t: http(base_url, "GET", f"/api/export/{t}.csv") for t in ("observations", "llm_calls")}
    summary = analyse(runs, exported, out_dir)
    summary.update({**meta, "set": os.path.basename(out_dir).rsplit("-", 1)[-1],
                    "problems": sorted({p for p, _ in grouped}), "variants": sorted({m for _, m in grouped})})
    with open(os.path.join(out_dir, "summary.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2, ensure_ascii=False)
    with open(os.path.join(out_dir, "summary.md"), "w", encoding="utf-8") as f:
        f.write(markdown(summary))
    print(markdown(summary))


def analyse(runs, exported, out_dir):
    observations_by_submission = defaultdict(list)
    for row in exported["observations"]:
        observations_by_submission[row["submission_id"]].append(row)
    calls_by_submission = defaultdict(list)
    for row in exported["llm_calls"]:
        calls_by_submission[row["submission_id"]].append(row)

    per_mode = {}
    obs_rows, ref_rows, review_b, hints_rows = [], [], [], []
    for problem_id, mode, chain, results in runs:
        m = per_mode.setdefault(mode, {
            "submissions": 0, "seconds": [], "llmStatus": Counter(), "costUsd": 0.0, "llmCalls": 0,
            "inputTokens": 0, "outputTokens": 0, "proposed": 0, "rejected": Counter(), "shown": 0,
            "shownMatched": 0, "referenceTotal": 0, "referenceFound": 0, "missedWeight": 0, "referenceWeight": 0,
            "shownMatchedLenient": 0, "referenceFoundLenient": 0, "missedWeightLenient": 0,
            "classificationChecked": 0, "classificationCorrect": 0, "leakRewritten": 0, "codeInAnswer": 0})
        previous = None
        for v, record in results:
            r = record["response"]
            sid = r["submissionId"]
            m["submissions"] += 1
            m["seconds"].append(record["seconds"])
            m["llmStatus"][r["llmStatus"]] += 1
            for call in calls_by_submission.get(sid, []):
                m["llmCalls"] += 1
                m["costUsd"] += float(call["cost_usd"])
                m["inputTokens"] += int(call["input_tokens"])
                m["outputTokens"] += int(call["output_tokens"])

            stored = observations_by_submission.get(sid, [])
            m["proposed"] += len(stored)
            for o in stored:
                if o["status"] == "rejected":
                    m["rejected"][o["reject_reason"]] += 1
                if int(o["leak_retries"] or 0) > 0:
                    m["leakRewritten"] += 1

            shown = r["observations"]
            refs = v["referenceObservations"]
            found = [False] * len(refs)
            found_lenient = [False] * len(refs)
            for o in stored:
                is_shown = o["shown"].lower() == "true"
                obs = {"type": o["type"], "line": int(o["line"]) if o["line"] else None}
                hit = [i for i, ref in enumerate(refs) if matches(obs, ref)]
                hit_lenient = [i for i, ref in enumerate(refs) if matches(obs, ref, lenient=True)]
                if is_shown:
                    m["shown"] += 1
                    m["shownMatched"] += bool(hit)
                    m["shownMatchedLenient"] += bool(hit_lenient)
                    for i in hit:
                        found[i] = True
                    for i in hit_lenient:
                        found_lenient[i] = True
                obs_rows.append({"problem": problem_id, "mode": mode, "version": v["version"], "type": o["type"],
                                 "line": o["line"], "status": o["status"], "reject_reason": o["reject_reason"],
                                 "shown": is_shown, "matches_reference": bool(hit),
                                 "matches_reference_lenient": bool(hit_lenient), "leak_retries": o["leak_retries"],
                                 "evidence_input": o["evidence_input_json"], "risk": o["risk"]})
            for s in shown:
                hints_rows.append({"problem": problem_id, "version": v["version"], "type": s["type"],
                                   "line": s["line"], "risk": s["risk"],
                                   "hint_1": s["hints"][0]["text"] if s["hints"] else "",
                                   "leaks_solution (manual)": ""})
            if mode == "proposed":
                for ref, ok, ok_lenient in zip(refs, found, found_lenient):
                    weight = SEVERITY_WEIGHT[ref["severity"]]
                    m["referenceTotal"] += 1
                    m["referenceWeight"] += weight
                    m["referenceFound"] += ok
                    m["referenceFoundLenient"] += ok_lenient
                    m["missedWeight"] += 0 if ok else weight
                    m["missedWeightLenient"] += 0 if ok_lenient else weight
                    ref_rows.append({"problem": problem_id, "version": v["version"], "type": ref["type"],
                                     "lines": ref["lines"], "severity": ref["severity"], "found": ok,
                                     "found_lenient": ok_lenient, "description": ref["description"]})
                if previous is not None and r["classification"] is not None:
                    m["classificationChecked"] += 1
                    if actual_classification(r["classification"]) == expected_classification(previous, v):
                        m["classificationCorrect"] += 1
            if mode == "baseline_b" and r["assistantMessage"]:
                if "```" in r["assistantMessage"]:
                    m["codeInAnswer"] += 1
                review_b.append({"problem": problem_id, "version": v["version"],
                                 "reference": " | ".join(f"{x['type']} L{x['lines']}: {x['description']}" for x in refs),
                                 "answer": r["assistantMessage"], "true_claims (manual)": "",
                                 "false_claims (manual)": "", "gives_solution (manual)": ""})
            previous = v

    write_csv(os.path.join(out_dir, "observations.csv"), obs_rows)
    write_csv(os.path.join(out_dir, "reference_matches.csv"), ref_rows)
    write_csv(os.path.join(out_dir, "baseline_b_review.csv"), review_b)
    write_csv(os.path.join(out_dir, "hints_review.csv"), hints_rows)

    summary = {"modes": {}}
    for mode, m in per_mode.items():
        s = {
            "submissions": m["submissions"],
            "llmStatus": dict(m["llmStatus"]),
            "llmCalls": m["llmCalls"],
            "tokens": {"input": m["inputTokens"], "output": m["outputTokens"]},
            "costUsd": round(m["costUsd"], 6),
            "costPerSubmissionUsd": round(m["costUsd"] / m["submissions"], 6) if m["submissions"] else None,
            "secondsMax": max(m["seconds"]) if m["seconds"] else None,
            "secondsMean": round(sum(m["seconds"]) / len(m["seconds"]), 2) if m["seconds"] else None,
        }
        if mode == "proposed":
            s.update({
                "proposedObservations": m["proposed"],
                "rejectedByReason": dict(m["rejected"]),
                "shownObservations": m["shown"],
                "precision": ratio(m["shownMatched"], m["shown"]),
                "recall": ratio(m["referenceFound"], m["referenceTotal"]),
                "missedSeverityWeighted": ratio(m["missedWeight"], m["referenceWeight"]),
                "precisionLenient": ratio(m["shownMatchedLenient"], m["shown"]),
                "recallLenient": ratio(m["referenceFoundLenient"], m["referenceTotal"]),
                "missedSeverityWeightedLenient": ratio(m["missedWeightLenient"], m["referenceWeight"]),
                "classificationAccuracy": ratio(m["classificationCorrect"], m["classificationChecked"]),
                "leakRewrites": m["leakRewritten"],
            })
        if mode == "baseline_b":
            s["answersWithCode"] = m["codeInAnswer"]
        summary["modes"][mode] = s
    return summary


def ratio(a, b):
    return round(a / b, 3) if b else None


def write_csv(path, rows):
    if not rows:
        open(path, "w").close()
        return
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def markdown(summary):
    lines = [f"# Evaluare {summary.get('runId', '')} ({summary.get('set', '')})", ""]
    for mode, s in summary["modes"].items():
        lines.append(f"## {mode}")
        for key, value in s.items():
            lines.append(f"- {key}: {value}")
        lines.append("")
    lines.append("Precizia și recall-ul sunt calculate automat doar pentru `proposed`; Baseline B se etichetează "
                 "manual în `baseline_b_review.csv`, iar scurgerile în `hints_review.csv`.")
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    main()
