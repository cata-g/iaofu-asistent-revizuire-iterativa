"""Execution harness that runs inside the Piston sandbox.

Files sent to Piston: harness.py (entry point), solution.py (code under test)
and, when the job has large inputs, inputs.py (the problem's generator).
The job arrives as JSON on stdin; the result is printed as one JSON line
prefixed with RESULT_MARKER, so prints from the solution cannot corrupt it.
"""
import contextlib
import copy
import hashlib
import importlib.util
import io
import json
import math
import random
import signal
import sys
import time
import traceback

RESULT_MARKER = "__HARNESS_RESULT__"
MAX_REPR = 2000


class TestTimeout(Exception):
    pass


def _on_alarm(signum, frame):
    raise TestTimeout()


signal.signal(signal.SIGALRM, _on_alarm)


def load_module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def canon(value, mode="exact"):
    """Normalizes a value so equal answers compare equal under the comparator."""
    if isinstance(value, float):
        if math.isnan(value) or math.isinf(value):
            return str(value)
        return round(value, 6)
    if isinstance(value, (set, frozenset)):
        return sorted((canon(v) for v in value), key=_sort_key)
    if isinstance(value, (list, tuple)):
        if mode == "unordered":
            return sorted((canon(v) for v in value), key=_sort_key)
        if mode == "unordered_nested":
            return sorted((canon(v, "unordered") for v in value), key=_sort_key)
        return [canon(v) for v in value]
    if isinstance(value, dict):
        return {str(k): canon(v) for k, v in value.items()}
    if value is None or isinstance(value, (bool, int, str)):
        return value
    return repr(value)


def _sort_key(value):
    return json.dumps(value, sort_keys=True)


def digest(value):
    return hashlib.sha256(_sort_key(value).encode()).hexdigest()[:16]


def short(value):
    text = json.dumps(value)
    return value if len(text) <= MAX_REPR else text[:MAX_REPR] + "..."


def error_text(exc):
    """Last frame inside solution.py plus the exception, e.g. 'line 4: IndexError: ...'."""
    frames = [f for f in traceback.extract_tb(exc.__traceback__) if f.filename.endswith("solution.py")]
    where = f"line {frames[-1].lineno}: " if frames else ""
    return f"{where}{type(exc).__name__}: {exc}"


def call(fn, params, args, limit_s):
    """Calls fn with a deep copy of args under a time limit; returns (value, error, ms)."""
    positional = [copy.deepcopy(args[p]) for p in params]
    captured = io.StringIO()
    signal.setitimer(signal.ITIMER_REAL, limit_s)
    start = time.perf_counter()
    try:
        with contextlib.redirect_stdout(captured):
            value = fn(*positional)
        return value, None, (time.perf_counter() - start) * 1000
    except TestTimeout:
        return None, "timeout", (time.perf_counter() - start) * 1000
    except RecursionError as exc:
        return None, error_text(exc), (time.perf_counter() - start) * 1000
    except Exception as exc:  # noqa: BLE001 - any student error is a test result
        return None, error_text(exc), (time.perf_counter() - start) * 1000
    finally:
        signal.setitimer(signal.ITIMER_REAL, 0)


def run_tests(fn, job):
    results = []
    for test in job.get("tests", []):
        value, error, ms = call(fn, job["params"], test["input"], job["testTimeoutMs"] / 1000)
        actual = None if error else canon(value, job["comparator"])
        expected = canon(test["expected"], job["comparator"])
        results.append({
            "id": test["id"],
            "group": test["group"],
            "passed": error is None and actual == expected,
            "actual": None if error else short(actual),
            "error": error,
            "timeMs": round(ms, 3),
        })
    return results


def run_large(fn, job):
    """Times fn on generated inputs of growing n; stops after the first timeout."""
    large = job.get("large")
    if not large:
        return []
    inputs = load_module("inputs", "inputs.py")
    timings = []
    timed_out = False
    for n in large["sizes"]:
        if timed_out:
            timings.append({"n": n, "status": "skipped", "timeMs": None, "digest": None})
            continue
        args = inputs.generate(n, random.Random(large["seed"] + n))
        best = None
        value, error = None, None
        for _ in range(large.get("repeats", 3)):
            value, error, ms = call(fn, job["params"], args, large["timeLimitMs"] / 1000)
            if error:
                break
            best = ms if best is None else min(best, ms)
            if ms > 50:
                break
        if error == "timeout":
            timed_out = True
            timings.append({"n": n, "status": "timeout", "timeMs": None, "digest": None})
        elif error:
            timings.append({"n": n, "status": "error", "timeMs": None, "digest": None, "error": error})
        else:
            timings.append({"n": n, "status": "ok", "timeMs": round(best, 3),
                            "digest": digest(canon(value, job["comparator"]))})
    return timings


def run_single(fn, job):
    """Runs one input (used by the counterexample verifier)."""
    value, error, ms = call(fn, job["params"], job["input"], job["testTimeoutMs"] / 1000)
    return {"actual": None if error else short(canon(value, job["comparator"])), "error": error,
            "timeMs": round(ms, 3)}


def validate(job):
    inputs = load_module("inputs", "inputs.py")
    try:
        return {"valid": bool(inputs.is_valid(job["input"]))}
    except Exception as exc:  # noqa: BLE001
        return {"valid": False, "error": f"{type(exc).__name__}: {exc}"}


def main():
    job = json.loads(sys.stdin.read())
    out = {"status": "ok"}
    if job["mode"] == "validate":
        out.update(validate(job))
    else:
        try:
            with contextlib.redirect_stdout(io.StringIO()):
                solution = load_module("solution", "solution.py")
            fn = getattr(solution, job["function"])
        except Exception as exc:  # noqa: BLE001 - syntax errors, missing function
            out = {"status": "compile_error", "error": error_text(exc) if not isinstance(exc, SyntaxError)
                   else f"line {exc.lineno}: SyntaxError: {exc.msg}"}
            print(RESULT_MARKER + json.dumps(out))
            return
        if job["mode"] == "single":
            out.update(run_single(fn, job))
        else:
            out["tests"] = run_tests(fn, job)
            out["timings"] = run_large(fn, job)
    print(RESULT_MARKER + json.dumps(out))


main()
