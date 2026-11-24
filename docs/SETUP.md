# Setup

About 20 minutes. Do this on setup night (Mon Jan 25) or before week 1.

## 1. Python 3.10 or newer

- **Windows:** install from python.org. On the first screen, tick **Add python.exe to PATH**.
  In a new terminal, `py --version` or `python --version` should print 3.10 or higher.
- **Mac:** install from python.org, then use `python3` wherever these guides say `python`.
- **Linux / Chromebook Linux:** `sudo apt install python3` (usually already there; use `python3`).

## 2. Git and a GitHub account

- Install git from git-scm.com (Mac: run `git --version` and accept the prompt).
- Make a free account at github.com and tell your instructor your user name so you can be given access.
- Tell git who you are, once:

```
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

## 3. An editor

VS Code (code.visualstudio.com) is a good default. Add the Python extension when it offers it.

## 4. Optional: Java 17 or newer

Only if you want to work in Java. Install a JDK (for example Temurin from adoptium.net).
`java --version` should print 17 or higher.

## 5. Get the repo and check

```
git clone <the repo link from your instructor>
cd hcc-bank-viewer-workshop
python tools/check.py --week 1
```

The setup lines should PASS. The data lines fail until you write code in week 1. That is expected.

## Common problems

| Problem | Fix |
|---|---|
| `python` is not recognized (Windows) | Use `py`, or reinstall Python with "Add to PATH" ticked. |
| `git clone` asks for a password | GitHub needs a token or GitHub Desktop now. Easiest: install GitHub Desktop and clone from there. |
| The repo link gives "not found" | It is private. Accept the invite email from GitHub first. |
| School laptop blocks installs | Use a GitHub Codespace from the repo page (Code button), which has Python ready. |
