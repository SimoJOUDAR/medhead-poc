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
