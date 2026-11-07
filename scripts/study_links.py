"""Prints the study links per participant, following the rotation in PRD 10.2.

Usage:
    python scripts/study_links.py two-sum valid-parentheses merge-intervals
    python scripts/study_links.py P1 P2 P3 --base-url http://192.168.1.10:5173

Each participant solves the three problems in order; the variant of each problem
follows the row for that participant. The link sets the variant and the participant
id, which the app keeps for the tab and removes from the address bar.
"""
import argparse

ROTATION = [
    ("proposed", "baseline_a", "baseline_b"),
    ("proposed", "baseline_b", "baseline_a"),
    ("baseline_a", "proposed", "baseline_b"),
    ("baseline_a", "baseline_b", "proposed"),
    ("baseline_b", "proposed", "baseline_a"),
    ("baseline_b", "baseline_a", "proposed"),
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("problems", nargs=3, help="ids of P1, P2, P3")
    parser.add_argument("--base-url", default="http://localhost:5173")
    parser.add_argument("--participants", type=int, default=6)
    args = parser.parse_args()

    for i in range(args.participants):
        modes = ROTATION[i % len(ROTATION)]
        print(f"Participant P{i + 1}")
        for slot, (problem, mode) in enumerate(zip(args.problems, modes), start=1):
            print(f"  {slot}. {args.base_url}/problems/{problem}?mode={mode}&participant=P{i + 1}   ({mode})")
        print()


if __name__ == "__main__":
    main()
