#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path
from typing import Any
import yaml

ROOT = Path(__file__).resolve().parents[2]
PROJECT_DIR = ROOT / ".github" / "project"

def load(name: str) -> Any:
    return yaml.safe_load((PROJECT_DIR / name).read_text(encoding="utf-8"))

def reqs(text: str | None) -> set[int]:
    if not text:
        return set()
    result: set[int] = set()
    for a, b in re.findall(r"REQ-(\d{3})(?:→REQ-(\d{3}))?", str(text)):
        start = int(a)
        end = int(b) if b else start
        result.update(range(start, end + 1))
    return result

def fail(msg: str) -> None:
    raise SystemExit(f"VALIDATION FAILED: {msg}")

def main() -> int:
    cfg = load("project.yml")
    deliverables = load("deliverables.yml")["deliverables"]
    bundles = load("bundles.yml")["bundles"]
    tasks = load("tasks.yml")["tasks"]
    optional = load("optional.yml")["optional_backlog"]

    if (len(deliverables), len(bundles), len(tasks), len(optional)) != (4, 13, 32, 4):
        fail(f"unexpected counts: deliverables={len(deliverables)}, bundles={len(bundles)}, tasks={len(tasks)}, optional={len(optional)}")

    all_items = deliverables + bundles + tasks + optional
    ids = [x["id"] for x in all_items]
    if len(ids) != len(set(ids)):
        fail("duplicate item IDs")

    members = set(cfg["members"])
    bundle_ids = {x["id"] for x in bundles}
    task_ids = {x["id"] for x in tasks}
    deliverable_ids = {x["id"] for x in deliverables}

    if deliverable_ids != {"DEL-001", "DEL-002", "DEL-003", "DEL-004"}:
        fail(f"unexpected deliverables: {sorted(deliverable_ids)}")

    expected_bundles = {f"W{i}" for i in range(13)}
    if bundle_ids != expected_bundles:
        fail(f"bundle IDs must be W0..W12; got {sorted(bundle_ids)}")

    for b in bundles:
        if b["deliverable"] not in deliverable_ids:
            fail(f"{b['id']} references unknown deliverable {b['deliverable']}")
        if b["lead"] not in members or b["reviewer"] not in members:
            fail(f"{b['id']} has invalid lead/reviewer")
        if not b.get("start_date") or not b.get("end_date"):
            fail(f"{b['id']} must carry bundle dates")
        if b["start_date"] > b["end_date"]:
            fail(f"{b['id']} start date is after end date")

    order_seen: dict[str, set[int]] = {}
    covered: set[int] = set()
    for t in tasks:
        if t["bundle"] not in bundle_ids:
            fail(f"{t['id']} references unknown bundle {t['bundle']}")
        if t["owner"] not in members or t["reviewer"] not in members:
            fail(f"{t['id']} has invalid owner/reviewer")
        if not (1 <= int(t["difficulty"]) <= 5):
            fail(f"{t['id']} difficulty outside 1..5")
        if t["priority"] not in {"P0", "P1", "P2"}:
            fail(f"{t['id']} invalid priority")
        if t["scope"] not in {"Mandatory", "Committed Optional", "Target Optional"}:
            fail(f"{t['id']} invalid scope")
        order_seen.setdefault(t["bundle"], set())
        if int(t["order"]) in order_seen[t["bundle"]]:
            fail(f"duplicate order {t['order']} inside {t['bundle']}")
        order_seen[t["bundle"]].add(int(t["order"]))
        for dep in t.get("dependencies") or []:
            if dep not in task_ids:
                fail(f"{t['id']} depends on unknown task {dep}")
        covered |= reqs(t.get("requirements"))

    mandatory_requirements = set(range(1, 17)) | set(range(19, 26))
    missing = sorted(mandatory_requirements - covered)
    if missing:
        fail(f"mandatory requirement coverage missing: {missing}")

    # Latest balanced plan invariants.
    owner_counts = {name: 0 for name in members}
    difficulty_totals = {name: 0 for name in members}
    for t in tasks:
        owner_counts[t["owner"]] += 1
        difficulty_totals[t["owner"]] += int(t["difficulty"])
    if owner_counts != {"Miguel Alves": 16, "Martim Gomes": 16}:
        fail(f"unexpected task balance: {owner_counts}")
    if difficulty_totals != {"Miguel Alves": 62, "Martim Gomes": 62}:
        fail(f"unexpected difficulty balance: {difficulty_totals}")

    # Dependency graph must be acyclic.
    graph = {t["id"]: list(t.get("dependencies") or []) for t in tasks}
    visiting: set[str] = set()
    visited: set[str] = set()
    def visit(node: str) -> None:
        if node in visited:
            return
        if node in visiting:
            fail(f"dependency cycle detected at {node}")
        visiting.add(node)
        for dep in graph[node]:
            visit(dep)
        visiting.remove(node)
        visited.add(node)
    for node in graph:
        visit(node)

    if "Start Date" not in cfg["fields"] or "End Date" not in cfg["fields"]:
        fail("Project must keep bundle/deliverable date fields")
    if "Difficulty" not in cfg["fields"]:
        fail("Project must contain Difficulty field")
    if "Owner" not in cfg["fields"] or "Reviewer" not in cfg["fields"]:
        fail("Project must contain full-name Owner/Reviewer fields")

    # Notification-safety / automation guardrails.
    dependabot = ROOT / ".github" / "dependabot.yml"
    if dependabot.exists():
        fail(".github/dependabot.yml must not exist in the notification-safe base")

    workflow_path = ROOT / ".github" / "workflows" / "project-bootstrap.yml"
    workflow_text = workflow_path.read_text(encoding="utf-8")
    forbidden_triggers = ["\n  push:", "\n  pull_request:", "\n  schedule:", "\n  issues:", "\n  workflow_run:"]
    for trigger in forbidden_triggers:
        if trigger in workflow_text:
            fail(f"project bootstrap workflow contains automatic trigger {trigger.strip()}")
    if "workflow_dispatch:" not in workflow_text:
        fail("project bootstrap workflow must remain manual-only")

    bootstrap_text = (PROJECT_DIR / "bootstrap.py").read_text(encoding="utf-8")
    forbidden_notification_actions = [
        "--add-assignee", "--remove-assignee", "--assignee",
        '"issue", "close"', '"issue", "reopen"',
        "@MarsGomes", "@MAlves4018",
    ]
    for token in forbidden_notification_actions:
        if token in bootstrap_text:
            fail(f"bootstrap contains notification-risk action/token: {token}")


    forbidden_fresh_repo_tokens = [
        "replace_safe", "prune_legacy", "remove_obsolete_fields",
        "close_legacy", "LEGACY |",
    ]
    for token in forbidden_fresh_repo_tokens:
        if token in bootstrap_text:
            fail(f"fresh-repository bootstrap contains migration/replacement logic: {token}")
    print("VALIDATION PASS")
    print(f"  deliverables: {len(deliverables)}")
    print(f"  bundles:      {len(bundles)}")
    print(f"  tasks:        {len(tasks)}")
    print(f"  optional:     {len(optional)}")
    print(f"  mandatory requirement coverage: {len(mandatory_requirements)}/{len(mandatory_requirements)}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
