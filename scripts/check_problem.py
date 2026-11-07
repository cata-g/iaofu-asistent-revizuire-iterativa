"""Checks a problem directory against the contract by running it through Piston.

Usage:
    python scripts/check_problem.py problems/two-sum              # check the problem and its chain
    python scripts/check_problem.py problems/two-sum --run v1.py  # run a solution, print the raw harness result
    python scripts/check_problem.py problems/two-sum --failures v1.py  # list the tests a solution fails

Checks: required fields, the reference passes every test and finishes every n,
every test input and example is valid for inputs.is_valid, and every version in
chain/chain.json fails exactly its expectedFailures.
Needs Piston running (docker compose up piston piston-init). Stdlib only.
"""
import argparse
import json
import os
import sys
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HARNESS = os.path.join(ROOT, "backend", "src", "main", "resources", "harness", "harness.py")
PISTON_URL = os.environ.get("PISTON_URL", "http://localhost:2000")
MARKER = "__HARNESS_RESULT__"
REQUIRED = ["id", "title", "category", "difficulty", "statement", "examples", "signature", "constraints",
            "timeLimitMs", "comparator", "forbiddenTerms", "tests", "large"]


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def run(problem_dir, solution_code, job, timeout_ms=20000):
    files = [{"name": "harness.py", "content": read(HARNESS)},
             {"name": "solution.py", "content": solution_code},
             {"name": "inputs.py", "content": read(os.path.join(problem_dir, "inputs.py"))}]
    body = json.dumps({"language": "python", "version": "3.12.0", "files": files,
                       "stdin": json.dumps(job), "run_timeout": timeout_ms,
                       "run_memory_limit": 512_000_000}).encode()
    req = urllib.request.Request(PISTON_URL + "/api/v2/execute", body, {"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        run_result = json.load(resp)["run"]
    for line in run_result["stdout"].splitlines():
        if line.startswith(MARKER):
            return json.loads(line[len(MARKER):])
    raise RuntimeError(f"no harness result; status={run_result.get('status')} stderr={run_result['stderr'][:500]}")


def evaluate_job(problem):
    return {"mode": "evaluate", "function": problem["signature"]["function"],
            "params": [p["name"] for p in problem["signature"]["params"]],
            "comparator": problem["comparator"], "testTimeoutMs": 2000,
            "tests": [{"id": t["id"], "group": t["group"], "input": t["input"], "expected": t["expected"]}
                      for t in problem["tests"]],
            "large": {**problem["large"], "timeLimitMs": problem["timeLimitMs"], "repeats": 3}}


def failures(problem_dir, problem, code, reference):
    """Test ids a solution fails, including large-<n> (timeout, error or a digest that differs)."""
    result = run(problem_dir, code, evaluate_job(problem))
    if result["status"] != "ok":
        return ["compile_error"]
    ref_digest = {t["n"]: t["digest"] for t in reference["timings"]}
    failed = [t["id"] for t in result["tests"] if not t["passed"]]
    failed += [f"large-{t['n']}" for t in result["timings"]
               if t["status"] != "ok" or t["digest"] != ref_digest[t["n"]]]
    return failed


def check_chain(problem_dir, problem, reference):
    path = os.path.join(problem_dir, "chain", "chain.json")
    if not os.path.exists(path):
        return ["missing chain/chain.json"]
    chain = json.loads(read(path))
    errors = []
    for v in chain["versions"]:
        code = read(os.path.join(problem_dir, "chain", v["file"]))
        actual = failures(problem_dir, problem, code, reference)
        expected = v["expectedFailures"]
        status = "ok" if sorted(actual) == sorted(expected) else "MISMATCH"
        print(f"  chain v{v['version']}: fails {actual} [{status}]")
        if status != "ok":
            errors.append(f"chain v{v['version']} fails {sorted(actual)}, expected {sorted(expected)}")
        for o in v["referenceObservations"]:
            if o["type"] not in ("correctness", "edge_case", "efficiency") or o["severity"] not in ("high", "medium", "low"):
                errors.append(f"chain v{v['version']}: bad reference observation {o}")
    return errors


def check(problem_dir):
    problem = json.loads(read(os.path.join(problem_dir, "problem.json")))
    errors = [f"missing field: {f}" for f in REQUIRED if f not in problem]
    for name in ["reference.py", "inputs.py"]:
        if not os.path.exists(os.path.join(problem_dir, name)):
            errors.append(f"missing file: {name}")
    if errors:
        return errors
    if problem["id"] != os.path.basename(os.path.normpath(problem_dir)):
        errors.append("id must match the directory name")

    result = run(problem_dir, read(os.path.join(problem_dir, "reference.py")), evaluate_job(problem))
    if result["status"] != "ok":
        return errors + [f"reference does not load: {result.get('error')}"]
    for t in result["tests"]:
        if not t["passed"]:
            errors.append(f"reference fails {t['id']}: actual={t['actual']} error={t['error']}")
    for timing in result["timings"]:
        if timing["status"] != "ok":
            errors.append(f"reference does not finish n={timing['n']} in {problem['timeLimitMs']} ms: {timing}")
        else:
            print(f"  reference n={timing['n']}: {timing['timeMs']} ms")

    for t in problem["tests"]:
        valid = run(problem_dir, "", {"mode": "validate", "input": t["input"]})
        if not valid["valid"]:
            errors.append(f"test {t['id']} is not valid for inputs.is_valid: {valid.get('error', '')}")
    for ex in problem["examples"]:
        valid = run(problem_dir, "", {"mode": "validate", "input": ex["input"]})
        if not valid["valid"]:
            errors.append(f"example {ex['input']} is not valid for inputs.is_valid")
    return errors + check_chain(problem_dir, problem, result)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("problem_dir")
    parser.add_argument("--run", help="solution file to run instead of checking the problem")
    parser.add_argument("--failures", help="solution file whose failing tests to list")
    args = parser.parse_args()
    if args.failures:
        problem = json.loads(read(os.path.join(args.problem_dir, "problem.json")))
        reference = run(args.problem_dir, read(os.path.join(args.problem_dir, "reference.py")), evaluate_job(problem))
        print(failures(args.problem_dir, problem, read(args.failures), reference))
        return
    if args.run:
        problem = json.loads(read(os.path.join(args.problem_dir, "problem.json")))
        print(json.dumps(run(args.problem_dir, read(args.run), evaluate_job(problem)), indent=2, ensure_ascii=False))
        return
    print(f"checking {args.problem_dir}")
    errors = check(args.problem_dir)
    for e in errors:
        print("  ERROR", e)
    print("  OK" if not errors else f"  {len(errors)} error(s)")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
