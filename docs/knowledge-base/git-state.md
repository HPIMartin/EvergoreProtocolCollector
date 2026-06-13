# 07 — Git State & the Uncommitted Diff

## Branch

Working on **`Rebuild`** (tracking `origin/Rebuild`). Main branch is `master`.

## History narrative (oldest → newest)

The log shows a project written in fits and starts, with the author's own honest commit messages
("some more bad commits", "Temp", "Restarting project", "Test broken? But deployed on server on
03.05.2023"). Rough arc:

1. **Bootstrapping** — initial commit, first log-parsing logic, Selenium added + drivers, "Loading
   content from webpage", package moves.
2. **First restart** — `af01ac5 Restarting project`; deployed to a server on 2023-05-03 despite a
   broken test; switched output to an HTML page; reactivated the scheduled job.
3. **Feature growth** — banking evaluation for the overview, an `EvergoreItem` "database", storage
   & withdrawal parameters per category, Java 11 → 17 upgrade.
4. **Quality push (current)** — make `Item`/`Entry` records, replace `instanceof`/`switch` with the
   **visitor pattern**, refactor `Entry`/`TransferType`, "Add withdrawl and storage value
   calculation to evergoreitem" (`5618ed9`, current HEAD).

**Takeaway:** the recent commits show a deliberate move toward cleaner patterns (records, visitors,
value calc). The `Rebuild` branch continues that. History is messy but the *direction* is good.

## The uncommitted diff — the important truth

`git status` looks alarming (≈80 files modified). **It is not.** Measured two ways:

| Measure | Files | +/− |
|---------|------:|-----|
| Raw `git diff HEAD` | 80 | 4533 / 4619 |
| `git diff HEAD --ignore-all-space` | **15** | **155 / 241** |

So **~95% of the diff is line-ending / whitespace churn** (CRLF↔LF). There is **no `.gitattributes`**
and `core.autocrlf` is unset — the classic Windows cause. The whitespace flip touches even `mvnw`,
`Dockerfile`, and binary drivers.

### The 15 files with *real* changes — one coherent feature

The actual change is **"extend `EvergoreDataEvaluator` from bank-only to bank + storage (Lager) value
calculation"**, plus housekeeping:

- `dataExtraction/EvergoreDataEvaluator.java` (+94) — storage evaluation logic (the meat).
- `businessLogic/metaInformation/MetaInformationKey.java` (+25) — new storage placement/withdrawl keys.
- `businessLogic/storage/StorageRepository.java` (+3) & `database/storage/StorageDatabaseRepository.java` (~41) — storage query methods (`getAllFor`, `getAllDifferentAvatars`).
- `businessLogic/base/TransferType.java`, `database/TransferTypeDatabaseVisitor.java`, `rest/controller/TransferTypeControllerVisitor.java` — small visitor tweaks.
- `dataExtraction/website/PageContentExtractor.java` (~13), `helper/selenium/Browser.java`, `helper/fileLoader/ResourceFileLoader.java` — minor.
- `pom.xml` (3) — small (e.g. dependency version).
- **Deletions / moves:** `Main.java` (−192, replaced by `Application.java`); `micronaut-cli.yml`
  moved into `src/main/resources/` (staged rename).
- **Binary:** `gecko-32/64-win.exe` upgraded to a newer geckodriver.
- **New untracked:** `src/test/.../EvergoreDataEvaluatorTest.java` (the stub for this feature).

### Untracked, non-source

`.claude/`, `.devcontainer/`, `.github/`, `.vscode/`, `docs/`, `logs.txt`, and the scratch files
this analysis created (`diag.txt`, `.agentscan*.txt` — safe to delete).

## Recommended git hygiene (see [../backlog.md](../backlog.md), epic A)

1. Add a **`.gitattributes`** (`* text=auto eol=lf`, mark `*.exe`/drivers binary) and set
   `core.autocrlf`; renormalize once and commit the whitespace normalization **in its own commit**.
2. Then the real 15-file change is tiny and reviewable — commit it as one clean feature commit
   ("Add storage value calculation to data evaluator") with its test.
3. Decide what to do with `logs.txt` and scratch files (gitignore / delete).
