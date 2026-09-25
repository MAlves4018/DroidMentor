#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parents[2]
PROJECT_DIR = ROOT / ".github" / "project"

MUTATION_SLEEP_SECONDS = float(os.environ.get("PROJECT_MUTATION_SLEEP_SECONDS", "0.15"))
RATE_CHECK_EVERY = int(os.environ.get("PROJECT_RATE_CHECK_EVERY", "40"))
MIN_GRAPHQL_REMAINING = int(os.environ.get("PROJECT_MIN_GRAPHQL_REMAINING", "250"))
_mutation_count = 0

ID_RX = re.compile(r"^\[([A-Z0-9-]+)\]")
MANAGED_PREFIXES = (
    "DEL-", "CORE-", "BR-", "SRCH-", "PROJ-", "RES-", "FILE-", "RSN-", "MOB-",
    "GATE-", "SETUP-", "UI-", "FLOW-", "M2-", "ARCH-", "NET-", "GEM-", "DB-",
    "M3-", "ORG-", "REL-", "OPT-",
)

def is_managed_id(value: str) -> bool:
    if value.startswith(MANAGED_PREFIXES):
        return True
    return bool(re.fullmatch(r"W(?:[0-9]|1[0-2])", value))


def load_yaml(name: str) -> Any:
    return yaml.safe_load((PROJECT_DIR / name).read_text(encoding="utf-8"))


def run(
    cmd: list[str],
    *,
    capture_json: bool = False,
    allow_failure: bool = False,
    stdin: str | None = None,
) -> Any:
    print("+", " ".join(cmd))
    cp = subprocess.run(cmd, cwd=ROOT, text=True, input=stdin, capture_output=True)
    if cp.stdout:
        print(cp.stdout.rstrip())
    if cp.stderr:
        print(cp.stderr.rstrip(), file=sys.stderr)
    if cp.returncode != 0 and not allow_failure:
        raise subprocess.CalledProcessError(cp.returncode, cmd, cp.stdout, cp.stderr)
    if capture_json and cp.returncode == 0:
        return json.loads(cp.stdout or "{}")
    return cp


def graphql(query: str, variables: dict[str, Any] | None = None) -> dict[str, Any]:
    payload = {"query": query, "variables": variables or {}}
    data = run(["gh", "api", "graphql", "--input", "-"], capture_json=True, stdin=json.dumps(payload))
    errors = data.get("errors") or []
    if errors:
        raise RuntimeError(f"GraphQL errors: {errors}")
    return data


def graphql_rate() -> dict[str, Any]:
    return graphql("query { rateLimit { limit remaining used resetAt } }")["data"]["rateLimit"]


def wait_for_graphql_budget(*, force: bool = False) -> None:
    global _mutation_count
    if not force and _mutation_count % RATE_CHECK_EVERY != 0:
        return
    rate = graphql_rate()
    remaining = int(rate["remaining"])
    reset_at = datetime.fromisoformat(rate["resetAt"].replace("Z", "+00:00"))
    print(f"RATE | remaining={remaining}/{rate['limit']} used={rate['used']} resetAt={rate['resetAt']}")
    if remaining >= MIN_GRAPHQL_REMAINING:
        return
    seconds = max(0, int((reset_at - datetime.now(timezone.utc)).total_seconds()) + 8)
    print(f"WAIT | GraphQL budget low; sleeping {seconds}s until reset.")
    time.sleep(seconds)


def after_mutation() -> None:
    global _mutation_count
    _mutation_count += 1
    if MUTATION_SLEEP_SECONDS:
        time.sleep(MUTATION_SLEEP_SECONDS)
    wait_for_graphql_budget()


def repo_parts() -> tuple[str, str]:
    value = os.environ.get("GITHUB_REPOSITORY", "")
    if "/" not in value:
        raise SystemExit("GITHUB_REPOSITORY is missing or invalid.")
    return tuple(value.split("/", 1))  # type: ignore[return-value]


def ensure_project(owner: str, title: str) -> tuple[int, str]:
    data = run(["gh", "project", "list", "--owner", owner, "--limit", "100", "--format", "json"], capture_json=True)
    for project in data.get("projects", []):
        if project.get("title") == title:
            print(f"REUSE project #{project['number']} — {title}")
            return int(project["number"]), str(project["id"])
    created = run(["gh", "project", "create", "--owner", owner, "--title", title, "--format", "json"], capture_json=True)
    after_mutation()
    return int(created["number"]), str(created["id"])


def edit_project(project_no: int, owner: str, cfg: dict[str, Any]) -> None:
    run([
        "gh", "project", "edit", str(project_no), "--owner", owner,
        "--description", str(cfg.get("description", "")),
        "--readme", str(cfg.get("project_readme", "")),
    ])


def ensure_labels(repo_ref: str, labels: list[dict[str, str]]) -> None:
    for label in labels:
        run([
            "gh", "label", "create", label["name"], "--repo", repo_ref,
            "--color", label["color"], "--description", label["description"], "--force",
        ])


def ensure_milestones(repo_ref: str, milestones: list[dict[str, str]]) -> None:
    current = run(["gh", "api", f"repos/{repo_ref}/milestones?state=all&per_page=100"], capture_json=True)
    by_title = {m["title"]: m for m in current}
    for milestone in milestones:
        if milestone["title"] in by_title:
            number = by_title[milestone["title"]]["number"]
            run([
                "gh", "api", "--method", "PATCH", f"repos/{repo_ref}/milestones/{number}",
                "-f", f"description={milestone['description']}",
                "-f", f"due_on={milestone['due']}T23:59:59Z",
            ])
            continue
        run([
            "gh", "api", "--method", "POST", f"repos/{repo_ref}/milestones",
            "-f", f"title={milestone['title']}",
            "-f", f"description={milestone['description']}",
            "-f", f"due_on={milestone['due']}T23:59:59Z",
        ])


def list_fields(project_no: int, owner: str) -> dict[str, dict[str, Any]]:
    data = run([
        "gh", "project", "field-list", str(project_no), "--owner", owner,
        "--limit", "100", "--format", "json",
    ], capture_json=True)
    return {f["name"]: f for f in data.get("fields", [])}



def ensure_fields(project_no: int, owner: str, fields: dict[str, Any]) -> dict[str, dict[str, Any]]:
    by_name = list_fields(project_no, owner)
    for name, spec in fields.items():
        existing = by_name.get(name)
        if existing and isinstance(spec, list):
            existing_options = {str(x.get("name")) for x in (existing.get("options") or [])}
            missing_options = [x for x in spec if x not in existing_options]
            if missing_options:
                raise RuntimeError(
                    f"Project field {name!r} is missing options {missing_options}. "
                    "This fresh-repository bootstrap never deletes/recreates fields automatically."
                )
        if existing:
            continue
        if isinstance(spec, list):
            created = run([
                "gh", "project", "field-create", str(project_no), "--owner", owner,
                "--name", name, "--data-type", "SINGLE_SELECT",
                "--single-select-options", ",".join(spec), "--format", "json",
            ], capture_json=True)
        elif spec == "date":
            created = run([
                "gh", "project", "field-create", str(project_no), "--owner", owner,
                "--name", name, "--data-type", "DATE", "--format", "json",
            ], capture_json=True)
        elif spec == "number":
            created = run([
                "gh", "project", "field-create", str(project_no), "--owner", owner,
                "--name", name, "--data-type", "NUMBER", "--format", "json",
            ], capture_json=True)
        else:
            created = run([
                "gh", "project", "field-create", str(project_no), "--owner", owner,
                "--name", name, "--data-type", "TEXT", "--format", "json",
            ], capture_json=True)
        by_name[name] = created
        after_mutation()
    return list_fields(project_no, owner)


def existing_issues(repo_ref: str) -> dict[str, dict[str, Any]]:
    rows = run([
        "gh", "issue", "list", "--repo", repo_ref, "--state", "all", "--limit", "1000",
        "--json", "number,title,url,state,labels,milestone,parent",
    ], capture_json=True)
    result: dict[str, dict[str, Any]] = {}
    for row in rows:
        match = ID_RX.match(row["title"])
        if match:
            result[match.group(1)] = row
    return result


def sync_issue(
    repo_ref: str,
    existing: dict[str, dict[str, Any]],
    *,
    item_id: str,
    title: str,
    body: str,
    labels: list[str],
    milestone: str | None,
    parent_number: int | None,
    managed_labels: list[str],
) -> tuple[dict[str, Any], bool]:
    issue = existing.get(item_id)
    if issue is None:
        cmd = ["gh", "issue", "create", "--repo", repo_ref, "--title", title, "--body", body]
        for label in labels:
            cmd += ["--label", label]
        if milestone:
            cmd += ["--milestone", milestone]
        if parent_number:
            cmd += ["--parent", str(parent_number)]
        cp = run(cmd)
        url = (cp.stdout or "").strip().splitlines()[-1]
        issue = {
            "number": int(url.rstrip("/").split("/")[-1]),
            "url": url,
            "title": title,
            "state": "OPEN",
            "labels": [],
        }
        existing[item_id] = issue
        if parent_number:
            after_mutation()
        return issue, True

    if str(issue.get("state", "")).upper() != "OPEN":
        raise RuntimeError(
            f"Matching issue #{issue['number']} for {item_id} is closed. "
            "Fresh bootstrap will not reopen it automatically; resolve it manually first."
        )

    cmd = ["gh", "issue", "edit", str(issue["number"]), "--repo", repo_ref, "--title", title, "--body", body]
    if milestone:
        cmd += ["--milestone", milestone]
    else:
        cmd += ["--remove-milestone"]
    if parent_number:
        cmd += ["--parent", str(parent_number)]
    else:
        cmd += ["--remove-parent"]

    current_labels = {
        x.get("name") for x in (issue.get("labels") or [])
        if isinstance(x, dict) and x.get("name")
    }
    for label in sorted(current_labels.intersection(managed_labels)):
        cmd += ["--remove-label", label]
    for label in labels:
        cmd += ["--add-label", label]

    run(cmd)
    if parent_number:
        after_mutation()
    return issue, False


def project_item_id(repo_owner: str, repo_name: str, issue_number: int, project_id: str) -> str | None:
    query = """
    query($owner:String!, $repo:String!, $number:Int!) {
      repository(owner:$owner, name:$repo) {
        issue(number:$number) {
          projectItems(first:50) { nodes { id project { id } } }
        }
      }
    }
    """
    data = graphql(query, {"owner": repo_owner, "repo": repo_name, "number": issue_number})
    for node in data["data"]["repository"]["issue"]["projectItems"]["nodes"]:
        if node.get("project", {}).get("id") == project_id:
            return str(node["id"])
    return None


def add_to_project(
    project_no: int,
    project_id: str,
    owner: str,
    repo_owner: str,
    repo_name: str,
    issue: dict[str, Any],
) -> tuple[bool, str]:
    cp = run([
        "gh", "project", "item-add", str(project_no), "--owner", owner,
        "--url", issue["url"], "--format", "json",
    ], capture_json=True, allow_failure=True)
    if isinstance(cp, dict):
        after_mutation()
        return True, str(cp["id"])

    text = f"{cp.stdout}\n{cp.stderr}".lower()
    if "already exists in this project" in text or "content already exists in this project" in text:
        item_id = project_item_id(repo_owner, repo_name, int(issue["number"]), project_id)
        if not item_id:
            raise RuntimeError(f"Could not resolve Project item for {issue['url']}")
        return False, item_id
    raise subprocess.CalledProcessError(cp.returncode, cp.args, cp.stdout, cp.stderr)


def option_id(field_meta: dict[str, Any], option_name: str) -> str:
    for option in field_meta.get("options", []) or []:
        if option.get("name") == option_name:
            return str(option["id"])
    raise RuntimeError(f"Option {option_name!r} not found in field {field_meta.get('name')!r}")


def set_project_field(
    project_id: str,
    item_id: str,
    meta: dict[str, Any],
    value: Any,
    kind: str,
) -> None:
    if value is None or value == "" or value == "None":
        return
    cmd = [
        "gh", "project", "item-edit",
        "--id", item_id, "--project-id", project_id, "--field-id", str(meta["id"]),
    ]
    if kind == "date":
        cmd += ["--date", str(value)]
    elif kind == "number":
        cmd += ["--number", str(value)]
    elif kind == "text":
        cmd += ["--text", str(value)]
    else:
        cmd += ["--single-select-option-id", option_id(meta, str(value))]
    run(cmd)
    after_mutation()


def apply_fields(
    project_id: str,
    item_id: str,
    field_map: dict[str, dict[str, Any]],
    values: dict[str, tuple[Any, str]],
    *,
    initialize_stage: bool,
) -> None:
    for name, (value, kind) in values.items():
        if name == "Stage" and not initialize_stage:
            continue
        meta = field_map.get(name)
        if not meta:
            raise RuntimeError(f"Missing Project field: {name}")
        set_project_field(project_id, item_id, meta, value, kind)


def main() -> int:
    cfg = load_yaml("project.yml")
    deliverables = load_yaml("deliverables.yml")["deliverables"]
    bundles = load_yaml("bundles.yml")["bundles"]
    tasks = load_yaml("tasks.yml")["tasks"]
    optional = load_yaml("optional.yml")["optional_backlog"]

    repo_owner, repo_name = repo_parts()
    repo_ref = f"{repo_owner}/{repo_name}"
    project_owner = repo_owner if cfg.get("owner") in {None, "", "repository-owner"} else cfg["owner"]


    wait_for_graphql_budget(force=True)
    project_no, project_id = ensure_project(project_owner, cfg["name"])
    edit_project(project_no, project_owner, cfg)
    ensure_labels(repo_ref, cfg["labels"])
    ensure_milestones(repo_ref, cfg["milestones"])

    field_map = ensure_fields(project_no, project_owner, cfg["fields"])

    link = run(["gh", "project", "link", str(project_no), "--owner", project_owner, "--repo", repo_ref], allow_failure=True)
    if link.returncode != 0 and "already" not in f"{link.stdout}\n{link.stderr}".lower():
        raise subprocess.CalledProcessError(link.returncode, link.args, link.stdout, link.stderr)

    issues = existing_issues(repo_ref)
    managed_labels = [x["name"] for x in cfg["labels"]]

    deliverable_numbers: dict[str, int] = {}
    for d in deliverables:
        issue, created = sync_issue(
            repo_ref, issues, item_id=d["id"], title=f"[{d['id']}] {d['title']}", body=d["body"],
            labels=d["labels"], milestone=d["milestone"], parent_number=None,
            managed_labels=managed_labels,
        )
        deliverable_numbers[d["id"]] = int(issue["number"])
        added, project_item = add_to_project(project_no, project_id, project_owner, repo_owner, repo_name, issue)
        apply_fields(project_id, project_item, field_map, {
            "Stage": (d["status"], "select"),
            "Item Type": ("Deliverable", "select"),
            "Area": ("Milestone Delivery", "select"),
            "Scope": (d["scope"], "select"),
            "Priority": ("P0", "select"),
            "Deliverable": (d["id"], "select"),
            "Owner": ("Both", "select"),
            "Reviewer": ("Both", "select"),
            "Start Date": (d["start_date"], "date"),
            "End Date": (d["end_date"], "date"),
        }, initialize_stage=True)

    bundle_numbers: dict[str, int] = {}
    for b in bundles:
        issue, created = sync_issue(
            repo_ref, issues, item_id=b["id"], title=f"[{b['id']}] {b['title']}", body=b["body"],
            labels=b["labels"], milestone=b["milestone"], parent_number=deliverable_numbers[b["deliverable"]],
            managed_labels=managed_labels,
        )
        bundle_numbers[b["id"]] = int(issue["number"])
        added, project_item = add_to_project(project_no, project_id, project_owner, repo_owner, repo_name, issue)
        area = b.get("area")
        if not area:
            area = None
        apply_fields(project_id, project_item, field_map, {
            "Stage": (b["status"], "select"),
            "Item Type": ("Bundle", "select"),
            "Area": (area, "select"),
            "Scope": (b["scope"], "select"),
            "Priority": (b["priority"], "select"),
            "Deliverable": (b["deliverable"], "select"),
            "Owner": (b["lead"], "select"),
            "Reviewer": (b["reviewer"], "select"),
            "Start Date": (b["start_date"], "date"),
            "End Date": (b["end_date"], "date"),
        }, initialize_stage=True)

    for t in tasks:
        deliverable = next(b["deliverable"] for b in bundles if b["id"] == t["bundle"])
        issue, created = sync_issue(
            repo_ref, issues, item_id=t["id"], title=f"[{t['id']}] {t['title']}", body=t["body"],
            labels=t["labels"], milestone=t["milestone"], parent_number=bundle_numbers[t["bundle"]],
            managed_labels=managed_labels,
        )
        added, project_item = add_to_project(project_no, project_id, project_owner, repo_owner, repo_name, issue)
        apply_fields(project_id, project_item, field_map, {
            "Stage": (t["status"], "select"),
            "Item Type": ("Task", "select"),
            "Area": (t["area"], "select"),
            "Scope": (t["scope"], "select"),
            "Priority": (t["priority"], "select"),
            "Difficulty": (t["difficulty"], "number"),
            "Deliverable": (deliverable, "select"),
            "Owner": (t["owner"], "select"),
            "Reviewer": (t["reviewer"], "select"),
        }, initialize_stage=True)

    for o in optional:
        issue, created = sync_issue(
            repo_ref, issues, item_id=o["id"], title=f"[{o['id']}] {o['title']}", body=o["body"],
            labels=o["labels"], milestone=None, parent_number=None,
            managed_labels=managed_labels,
        )
        added, project_item = add_to_project(project_no, project_id, project_owner, repo_owner, repo_name, issue)
        apply_fields(project_id, project_item, field_map, {
            "Stage": (o["status"], "select"),
            "Item Type": ("Optional Backlog", "select"),
            "Area": (o["area"], "select"),
            "Scope": (o["scope"], "select"),
            "Priority": (o["priority"], "select"),
        }, initialize_stage=True)

    wait_for_graphql_budget(force=True)
    print(
        f"Bootstrap complete: {len(deliverables)} deliverables, {len(bundles)} bundles, "
        f"{len(tasks)} tasks, {len(optional)} optional backlog items."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
