# Setup

About 20 minutes. Do this before week 1 (Mon Feb 1).

## 1. Java 21 (a JDK)

Install **Temurin 21** from adoptium.net (pick the installer for your computer). In a new
terminal, `java --version` should print 21 or higher.

- **Windows:** in the installer, turn on **Set JAVA_HOME** and **Add to PATH**.
- **Mac:** the .pkg installer does everything.
- **Linux / Chromebook Linux:** `sudo apt install openjdk-21-jdk`.

You do not need to install Maven. The repo comes with `./mvnw`, which downloads the right
Maven the first time you run it (that first run takes a minute or two).

## 2. Git and a GitHub account

- Install git from git-scm.com (Mac: run `git --version` and accept the prompt).
- Make a free account at github.com and tell your instructor your user name so you can be given access.
- Tell git who you are, once:

```
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

## 3. An editor and an agent

IntelliJ IDEA Community or VS Code (with the Java extension pack) both work. Open the repo
folder and the editor finds `pom.xml` by itself. Use any AI coding agent you like; point it at
`AGENTS.md`.

## 4. Get the repo and check

```
git clone <the repo link from your instructor>
cd hcc-bank-viewer-workshop
./mvnw test -Dweek=1            # Windows: mvnw.cmd test -Dweek=1
```

You should see the build run, and the week 1 tests **fail** with messages like
`TODO week 1: write Money.clean`. That is expected: writing that code is week 1. If you see
those messages, your setup works.

## 5. Optional: Python 3.10 or newer

Only for the optional Python lane (`python-lane/`) and the instructor self test.

## Common problems

| Problem | Fix |
|---|---|
| `./mvnw: Permission denied` (Mac, Linux) | Run `chmod +x mvnw run` once. |
| `JAVA_HOME is not set` or the wrong Java runs | Reinstall Temurin with "Set JAVA_HOME" on, then open a new terminal. |
| The first `./mvnw` run is slow | It downloads Maven and the libraries once. Later runs are fast. |
| `git clone` asks for a password | GitHub needs a token or GitHub Desktop now. Easiest: install GitHub Desktop and clone from there. |
| The repo link gives "not found" | It is private. Accept the invite email from GitHub first. |
| School laptop blocks installs | Use a GitHub Codespace from the repo page (Code button). Check `java --version` there; if it is older than 21, tell your instructor. |
