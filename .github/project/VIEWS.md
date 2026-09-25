# GitHub Project views

The Project data is synchronized automatically, but saved views are configured once in the GitHub UI.

## Roadmap
- Layout: Roadmap
- Filter: `Item Type` is `Deliverable` or `Bundle`
- Start date: `Start Date`
- Target date: `End Date`
- Group by: `Deliverable`
- Markers: milestones

## Execution
- Layout: Board
- Filter: `Item Type = Task`
- Column field: `Stage`
- Show: Assignees, Priority, Area, Difficulty

## My Work
- Layout: Table
- Filter: `Item Type = Task`, `Assignee = @me`
- Exclude: Done, Deferred, Cut
- Sort: Priority, then parent bundle/order

## Deliverables
- Layout: Table
- Filter: `Item Type = Deliverable`
- Show: Stage, Start Date, End Date, Milestone, Assignees

## Bundles
- Layout: Table
- Filter: `Item Type = Bundle`
- Group by: Deliverable
- Show: Stage, Area, Owner, Reviewer, Start Date, End Date

## Mandatory
- Layout: Table
- Filter: `Scope = Mandatory`
- Group by: Deliverable
- Show: Item Type, Stage, Area, Priority, Owner, Reviewer

## Optional Backlog
- Layout: Table
- Filter: `Item Type = Optional Backlog`
- Show: Stage, Scope, Area, Priority

## By Student
- Layout: Table
- Filter: `Item Type = Task`
- Group by: Owner
- Show: Stage, Reviewer, Area, Priority, Difficulty, Parent issue
