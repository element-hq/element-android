## Installing from CI

<!--- TOC -->

  * [Installing from Buildkite](#installing-from-buildkite)
  * [Installing from GitHub](#installing-from-github)
    * [Create a GitHub token](#create-a-github-token)
  * [Provide artifact URL](#provide-artifact-url)
  * [Next steps](#next-steps)
  * [Future improvement](#future-improvement)

<!--- END -->

Installing APK build by the CI is possible

### Installing from Buildkite

The script `./tools/install/installFromBuildkite.sh` can be used, but Builkite will be removed soon. See next section.

### Installing from GitHub

To install an APK built by a GitHub action, run the script `./tools/install/installFromGitHub.sh`. You will need to pass a GitHub token to do so.

#### Create a GitHub token

You can create a GitHub token going to your Github account, at this page: [https://github.com/settings/tokens](https://github.com/settings/tokens).

You need to create a token (classic) with the scope `repo/public_repo`. So just check the corresponding checkbox.
Validity can be long since the scope of this token is limited. You will still be able to delete the token and generate a new one.
Click on Generate token and save the token locally.

### Provide artifact URL

The script will ask for an artifact URL. You can get this artifact URL by following these steps:

- open the pull request
- in the check at the bottom, click on `APK Build / Build debug APKs`
- click on `Summary`
- scroll to the bottom of the page
- copy the link `vector-Fdroid-debug` if you want the F-Droid variant or `vector-Gplay-debug` if you want the Gplay variant.

The copied link can be provided to the script.

### Next steps

The script will download the artifact, unzip it and install the correct version (regarding arch) on your device.

Files will be added to the folder `./tmp/DebugApks`. Feel free to cleanup this folder from time to time, the script will not delete files.

### Future improvement

The script could ask the user for a Pull Request number and Gplay/Fdroid choice like it was done with Buildkite script. Using GitHub API may be possible to do that.
