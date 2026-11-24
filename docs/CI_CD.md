# CI and CD, built by your AI agent

Every week you also grow a small **pipeline**: a robot that checks your work on every push (CI) and,
from week 4, publishes your viewer when you tag a release (CD). You do not write the YAML by hand.
You **ask your AI agent for it, then review what it gave you.** Reviewing is the skill.

## The words, in one minute

| Word | Means |
|---|---|
| **CI** (continuous integration) | Every push is tested automatically, so a broken change is caught in minutes, not on demo night. |
| **CD** (continuous delivery / deployment) | A passing build is published automatically. For us: a tag like `v1.0.0` puts your viewer on a web page. |
| **Workflow** | A YAML file in `.github/workflows/`. GitHub Actions reads it. |
| **Trigger** (`on:`) | What starts it: a push, a pull request, a tag, or a button. |
| **Job / step** | A job runs on a fresh machine (a **runner**). Steps run in order; the first red step stops the job. |
| **Action** (`uses:`) | A ready made step, like `actions/checkout@v4`. The `@v4` pins the version. |
| **Artifact** | A file a run keeps for you to download, like your `transactions.json`. |

## How the weeks build it

| Week | Pipeline step | Verify with |
|---|---|---|
| 1 | CI: run the structurer and `check.py --week 1` on every push | a green check mark on your commit |
| 2 | Break it on purpose, watch it go red, fix it | a red X, then green again |
| 3 | Run CI on pull requests too, and keep `transactions.json` as an artifact | a check on your pull request |
| 4 | CD: a `v*` tag tests, builds `_site/` and deploys it to GitHub Pages | your viewer at its Pages address |
| 5 | Your choice: Java in CI, a nightly run, a status badge | whatever you added runs green |

The prompts for each week are in that week's guide. Reference answers live in `reference/ci/`.
Peek after your agent has written its version, not before.

## Review the AI's workflow: the checklist

Read every line of what your agent wrote and check:

1. **The trigger is what you asked for.** CI on `push` and `pull_request`; deploy only on tags `v*`.
2. **Permissions are small.** `contents: read` for checks. Only the deploy job gets `pages: write` and `id-token: write`.
3. **Every `uses:` is a well known action with a version**, like `actions/checkout@v4`. Ask about anything from an account you do not recognize.
4. **No secrets, keys or passwords are in the file.** This pipeline needs none.
5. **It runs the same commands you run by hand.** If it does something you cannot explain, ask the agent to explain that line.
6. **It fails when it should.** Break a test on purpose once and watch it go red.
7. **There is a `timeout-minutes`**, so a stuck run cannot burn your free minutes.

Ask your agent: *"Explain this workflow to me line by line, like I am new to this."* You should be able to explain it back.

## When GitHub Actions does not start

Sometimes a run never starts. You may see "The job was not started because recent account payments have failed
or your spending limit needs to be increased", or the run sits in "Queued" for a long time.

1. **Run the same pipeline on your computer:**

   ```
   python tools/ci_local.py --week 1
   ```

   It runs exactly the steps your workflow runs and stops at the first red one, just like Actions.
2. **For CD, build and preview the site locally:**

   ```
   python tools/ci_local.py --week 4
   python tools/serve.py _site
   ```

   `_site/` is exactly what the deploy uploads, so if it works here, the deploy has what it needs.
3. **Keep going with the week.** A run that never started is an account problem, not a problem with your code.
   Tell your instructor, and push anyway: the workflow will run once Actions works again.

Other common causes:

| You see | Likely cause |
|---|---|
| No Actions tab, or "Actions disabled" | Settings > Actions > General > allow actions. |
| The workflow never appears | The file is not in `.github/workflows/`, or the YAML has an indentation error. Ask your agent to check it. |
| Pages deploy fails with "not allowed to deploy to github-pages due to environment protection rules" | Settings > Environments > github-pages > add a rule that allows tags `v*`. |
| Pages is not offered for your repo | Pages on a **private** repo needs a paid plan or the free GitHub Student Developer Pack. Use the local preview, or ask your instructor. |

Remember: a Pages site is a public web page, even when the repo is private. That is fine here because every number is fake.
Never deploy real bank data.
