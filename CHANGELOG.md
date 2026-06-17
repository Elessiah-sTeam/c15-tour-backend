# Changelog

## [0.2.0](https://github.com/Elessiah-sTeam/c15-tour-backend/compare/v0.1.0...v0.2.0) (2026-06-17)


### Features

* accept organiser or share code on redirect endpoint ([ea790f3](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/ea790f35286642ea399d754f1c4673a8d7eaa790))
* accept organiser or share code on redirect endpoint ([552a178](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/552a178b7cd38ebfc80ba2aebb8f5f0b8ee73cb8))
* add mobile redirect endpoint using OSRM table + route ([9a299b6](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/9a299b66988a1e5b92c2fcb4fb24b3ef43baa42b))
* add mobile redirect endpoint using OSRM table + route ([295f307](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/295f30779f981259684aa9cfce033608c490a204))
* **auth:** email-based auth with change/forgot/reset-password + Mailjet ([5387d52](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/5387d52f35a6ba181cfba2911ffc2c938cc06c86))
* **auth:** implement email-based auth with password reset via Mailjet ([06e18a1](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/06e18a16c8aa5a4e5c615c79dcbcb68306ded08d))
* change redirect response schema to include tour and route data ([127ce4b](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/127ce4b13ecaeeaa3fe1c29c111ee8e4d78d48d3))
* change redirect response schema to include tour and route data ([4756cdb](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/4756cdb4ef160becc1ae47253457ccee5fa90c20))
* endpoint DELETE /auth/account (suppression de compte) ([#145](https://github.com/Elessiah-sTeam/c15-tour-backend/issues/145)) ([bc8f06f](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/bc8f06f0b32b9c51ecb08fa2ba5a55f111cb0cee))
* include directions steps in route-to-start response ([b2f5e69](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/b2f5e69595e7b1a9918735acb78b1e5b5c787534))
* include directions steps in route-to-start response ([adb098b](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/adb098b9c885a841473c0c54b16e2dcc2f8502f1))
* narrow redirect candidate window from 5 to 3 ([b949ce7](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/b949ce727b311a1b64eb6e2613cd5554bb366230))
* narrow redirect candidate window from 5 to 3 ([5c9ed97](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/5c9ed97c997de0c05671e8078ac19c48b53d7011))
* production readiness — env-var CORS, fix logging, remove debug … ([f9c3d8a](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/f9c3d8ab16ca6e0c73306fcff77c102245577676))
* production readiness — env-var CORS, fix logging, remove debug startup code ([9cdf010](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/9cdf010a09b21897675e67a1b6e54e50cd1f9461))
* redirect returns full route through remaining waypoints ([bd85fd2](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/bd85fd2b0dbc584e518288a216b7fd342e4d8ed1))
* route through optimal rejoin waypoint then continue to end ([1db139d](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/1db139d34f456c0d1446a8040081b3139f5ef8c3))
* scope convoy history per account with owner FK and role enum ([2787895](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/2787895c93d7a9bcbea9eb177b4ff9bbc19fe5a9))
* scope convoy history per account with owner FK and role enum ([ba023c9](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/ba023c9c208db04bb7ab00a5914895696d01bf91))


### Bug Fixes

* add WHERE clause to V18 migration UPDATE to satisfy SonarQube S4826 ([b8717c0](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/b8717c079184032b3ea1b5bcaf2898972e66229e))
* allow empty Mailjet keys in CI to prevent context load failure ([9a19669](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/9a1966908717e664c49bfb44287562504cdae061))
* **auth:** add input validation and reset_token index ([db332d4](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/db332d4ac20052bade62c4ada78bc355943219b1))
* **auth:** extract MESSAGE_KEY constant to fix SonarQube S1192 ([49a7939](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/49a7939ae27665e1c029ce552c0080d079466e0e))
* correct self-referential ROLE_ADMIN constant initializer ([99c4acc](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/99c4acc1087336af88608bd425a2ad37166a3267))
* extract ROLE_ADMIN constant to fix SonarQube S1192 ([d69cedb](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/d69cedbcd704277181fd3b30222738bd24912fee))
* remove redundant mailjet config from application-dev.yml ([44fcb09](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/44fcb09c19ad34fa0dee18afe94e86094805f34e))
* replace ReDoS-prone email regex with linear indexOf check (S5852) ([c2ec7b1](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/c2ec7b113b3c87b2624391f4696909eaa6b88389))
* **test:** restore RoutingService MockitoBean required by BackendApplication CommandLineRunner ([62360c3](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/62360c34e1b930248498d97132eb2ac28b800d97))
* use raw URI in OSRM Table call to prevent semicolon encoding ([56cf1fa](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/56cf1fa4633e08c4824fe79baa0da799bc911b0e))

## [0.1.0](https://github.com/Elessiah-sTeam/c15-tour-backend/compare/v0.0.6...v0.1.0) (2026-05-19)


### Features

* publish Docker image to GHCR on release ([b9b0a96](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/b9b0a96d1b472ed3c47000adbcc46b98a3150c22))
* publish Docker image to GHCR on release ([be10ca5](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/be10ca5bfee34decfcbfdc0c0ab76d267b38e2d9))


### Bug Fixes

* skip release-please on snapshot merges to prevent stale PRs ([40752b5](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/40752b558f76a997972617f7ea18a203d296c5b8))

## [0.0.6](https://github.com/Elessiah-sTeam/c15-tour-backend/compare/v0.0.5...v0.0.6) (2026-05-19)


### Bug Fixes

* restore auto-sync on release-please merges, only skip CI ([3716a90](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/3716a9098fa63f8fc55fed8314fe119588a7cc22))
* skip CI and sync on release-please bot merges ([5ecd09b](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/5ecd09ba4a7833be6adf179163b9d9a749ecad3b))
* skip CI and sync on release-please bot merges ([e79dd91](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/e79dd91bcb4b8984bf17a64a061257098a09e700))
* test ([0dd280e](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/0dd280e06efd3e384af5bb76554f6a6bdbf99a60))
* test ([f638a74](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/f638a74cef4a46eb3bf70c3059b97146affc5f2b))

## [0.0.5](https://github.com/Elessiah-sTeam/c15-tour-backend/compare/v0.0.4...v0.0.5) (2026-05-18)


### Bug Fixes

* create PR instead of direct push to protected dev branch ([0cfde63](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/0cfde6321c1bd835be34b086fdf585f905a61072))


## [0.0.4](https://github.com/Elessiah-sTeam/c15-tour-backend/compare/v0.0.3...v0.0.4) (2026-05-18)


### Bug Fixes

* add contents write permission to sync workflow ([a62c093](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/a62c0930a5c5fda8364ea5b0c6122edec235392d))

## [0.0.3](https://github.com/Elessiah-sTeam/c15-tour-backend/compare/v0.0.2...v0.0.3) (2026-05-18)


### Bug Fixes

* remove explicit version from backend module, inherit from parent ([dc2ab11](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/dc2ab11d08e3e5a87267005d4b3f59d3d1d6debb))

## [0.0.2](https://github.com/Elessiah-sTeam/c15-tour-backend/compare/v0.0.1...v0.0.2) (2026-05-18)


### Bug Fixes

* correct parent version and add groupId in backend/pom.xml ([84629d8](https://github.com/Elessiah-sTeam/c15-tour-backend/commit/84629d8dbd5c5e4009c4bfad4e12597f363f39d2))

## Changelog

Toutes les versions notables de ce projet seront documentées dans ce fichier.

Le format suit Keep a Changelog et la numérotation suit Semantic Versioning.
