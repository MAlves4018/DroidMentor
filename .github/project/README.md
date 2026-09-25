# GitHub Project bootstrap

This directory contains the version-controlled planning definition used to create and populate the **DroidMentor — 2026/27 Delivery** GitHub Project in a **fresh repository**.

The bootstrap is intentionally **manual-only**. Ordinary pushes, pull requests, issue activity and scheduled events do not run it.

## Notification-safety rules

The bootstrap deliberately does **not**:

- assign Miguel or Martim to repository issues;
- request reviews;
- `@mention` either student;
- close or reopen issues;
- delete or replace Project items;
- run on `push`, `pull_request`, `schedule`, `issues`, or `workflow_run` events;
- configure Dependabot.

Ownership and reviewer Owner information is stored in Project custom fields. Native GitHub issue assignment should only be used later for the small number of tasks that are actively being worked on.

## Recommended fresh-repository order

To minimize notification noise:

1. Create the new repository and push this base.
2. **Before inviting collaborators**, run the Project bootstrap if practical. This is the safest option because bulk issue creation cannot notify collaborators who are not yet subscribed to the repository.
3. In repository **Settings → Security and quality → Advanced Security**, disable Dependabot alerts/security updates if you do not want automated dependency notifications or security PRs for this academic repository. There is intentionally no `.github/dependabot.yml` in this base.
4. Add the `PROJECT_TOKEN` Actions secret.
5. Run **Actions → Bootstrap GitHub Project (Manual) → `validate`** first.
6. If validation passes, run it again with `mode = bootstrap` and tick `notifications_checked`.
7. After the Project has been created and populated, invite Martim and the required teachers/collaborators.
8. Each collaborator should keep the repository Watch setting at **Participating and @mentions** unless they explicitly want all issue activity.

The bootstrap is designed for first setup. It can be rerun after a partial setup failure before development starts, but it should not become a routine synchronization mechanism after people begin changing task status in GitHub.

## What it creates

The current declarative plan contains:

- 4 formal deliverables;
- 13 weekly bundles (`W0`–`W12`);
- 32 outcome-sized implementation tasks;
- 4 unscheduled optional-backlog items;
- milestones, labels and Project custom fields;
- parent relationships `Deliverable → Bundle → Task`;
- planning Owner/Reviewer fields without bulk native GitHub assignment.

Saved visual views are configured once in the GitHub UI. `VIEWS.md` documents their intended layout.

## After setup

The GitHub Project becomes the operational roadmap. The files in this directory remain useful as a reproducible planning snapshot, but the bootstrap workflow should not be used on ordinary development commits.
