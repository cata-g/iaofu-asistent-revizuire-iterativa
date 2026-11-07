"""Checks a problem directory against the contract by running it through Piston.

Usage:
    python scripts/check_problem.py problems/two-sum              # check the problem
    python scripts/check_problem.py problems/two-sum --run v1.py  # run a solution, print the raw harness result

Checks: required fields, the reference passes every test, every test input is
valid for inputs.is_valid, and the generator produces valid inputs for each n.
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
    return errors


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("problem_dir")
    parser.add_argument("--run", help="solution file to run instead of checking the problem")
    args = parser.parse_args()
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
