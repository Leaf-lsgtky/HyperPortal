# Pull request CI

`PR CI` automatically tests and builds every opened, updated, reopened, or
ready-for-review pull request, including forks. It builds the PR merged with its
current base so that integration failures are caught before merging. New commits
cancel an older build for the same PR.

To build an existing PR manually:

```sh
gh workflow run pr-ci.yml -f pr_number=13
```

The build runs the Python regression tests, the app's release unit tests, and
`python3 tools/build.py all --unsigned`. R8 remains disabled for these test builds.
The explicit `--unsigned` flag is only for CI intermediate APKs. Ordinary builds
still require `.sign/` and never fall back to a debug key.

After a successful build, `Sign PR APKs` runs from the default branch on a fresh
runner. It does not check out PR code, execute PR scripts, or restore PR caches.
It validates the unsigned artifact's file names and metadata, signs both APKs
with the existing repository release-key secrets, and verifies the signatures.
Signing secrets are never supplied to the PR build runner.

Download the `hyperportal-pr-<number>-signed-<build-run-id>-<attempt>` artifact from
the completed **Sign PR APKs** run. It contains `HyperPortal.apk`,
`ContentCatcher-patched.apk`, `build-info.json` (including the PR head and merge
commits), and `SHA256SUMS`. Signed artifacts are retained for 14 days; intermediate
unsigned APKs and test reports are retained for 7 days. Do not install or distribute
the unsigned intermediate artifacts.

This CI does not publish GitHub Releases, install on a device, or reboot anything.
The existing tagged release workflow is unchanged.
