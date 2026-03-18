# MedHead PoC -- Real-Time Emergency Hospital Bed Allocation

> Proof of Concept for the MedHead Consortium's real-time emergency response system.
> Recommends the nearest hospital with an available bed in the required medical specialty.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Running the Application](#running-the-application)
- [Running the Tests](#running-the-tests)
- [CI/CD Pipeline](#cicd-pipeline)
- [Branch workflow](#branch-workflow)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)

## Overview

<!-- To be completed in sessions S2-S5 -->

## Architecture

<!-- To be completed in sessions S2-S5 -->

## Prerequisites

<!-- To be completed in session S2 -->

## Getting Started

<!-- To be completed in session S2 -->

## Running the Application

<!-- To be completed in session S2 -->

### Local dev credentials

The backend ships a single pre-seeded user for local development and demo purposes:

| Username | Password |
|----------|----------|
| `demo`   | `demo`   |

Exchange them for a JWT via `POST /api/v1/auth/login`:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo"}'
```

The response payload includes a `token` field; send it as `Authorization: Bearer <token>` on every subsequent `/api/v1/**` call. Tokens are HS256-signed and expire after 60 minutes by default.

Production deployments must override the signing secret and user store via environment variables (`APP_SECURITY_JWT_SECRET`, `APP_SECURITY_USERS_0_PASSWORD`, `APP_SECURITY_JWT_TTL_MINUTES`). The default secret shipped in `application.yaml` is flagged as dev-only and must not leave a developer machine.

## Running the Tests

<!-- To be completed in session S4 -->

## CI/CD Pipeline

<!-- To be completed in session S5 -->

## Branch workflow

The project follows **GitHub Flow**: `master` is the only long-lived branch and is always deployable. Every change lands through a short-lived topic branch and a pull request.

### Branching

- Cut topic branches from `master`; delete them after merge (one branch per PR).
- Branch names use `type/short-description`, kebab-case, where `type` is one of `feat`, `fix`, `ci`, `docs`, `test`, `chore`, `refactor` (for example `ci/backend-workflow`, `feat/bed-availability`).

### Merging to `master`

Direct pushes to `master` are disabled. Every change reaches `master` through a pull request that satisfies all of the following, enforced by branch protection:

- The `backend-ci` and `frontend-ci` status checks are green.
- The branch is up to date with `master` before merging.
- Force-pushes and deletion of `master` are rejected.

### Commits

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `ci:`, `docs:`, `test:`, `chore:`, `refactor:`) so history stays scannable and can drive release notes later.

## API Documentation

<!-- To be completed in session S2 -->

## Project Structure

<!-- To be completed in session S2 -->
