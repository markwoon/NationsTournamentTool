### [1.3.4](https://github.com/markwoon/NationsTournamentTool/compare/v1.3.3...v1.3.4) (2021-05-12)


### Bug Fixes

* improve support for different date formats and reading tsv that has been exported from Excel ([8883135](https://github.com/markwoon/NationsTournamentTool/commit/88831355c8d8498e4642bf1eea93c05a86079cfe))

### [1.3.3](https://github.com/markwoon/NationsTournamentTool/compare/v1.3.2...v1.3.3) (2021-05-10)


### Bug Fixes

* fix TSV parsing when not all expected columns are present ([3763ba3](https://github.com/markwoon/NationsTournamentTool/commit/3763ba3cc00377786470a0fd3bbcff84ab29a283))

### [1.3.2](https://github.com/markwoon/NationsTournamentTool/compare/v1.3.1...v1.3.2) (2021-05-04)


### Bug Fixes

* avoid NPE when getting list ([886e79d](https://github.com/markwoon/NationsTournamentTool/commit/886e79def48161ca464c9ec8495970f2f4a0ea28))

### [1.3.1](https://github.com/markwoon/NationsTournamentTool/compare/v1.3.0...v1.3.1) (2021-04-28)


### Bug Fixes

* fix build by switching from logback to slf4j-nop ([75e5f93](https://github.com/markwoon/NationsTournamentTool/commit/75e5f9380cdc55a90ccc74d7af439386fd53c419))

## [1.3.0](https://github.com/markwoon/NationsTournamentTool/compare/v1.2.4...v1.3.0) (2021-04-28)


### Features

* add support for creating games from app ([0db17a6](https://github.com/markwoon/NationsTournamentTool/commit/0db17a64d046acb09f20164017edca27d27d8527))
* update exported columns and scoring calc ([1e85f7f](https://github.com/markwoon/NationsTournamentTool/commit/1e85f7fb1647596f52d8f5c22b4553a6d74e5f43))


### Bug Fixes

* handle expired games properly ([cdcba70](https://github.com/markwoon/NationsTournamentTool/commit/cdcba70f2ecc361951625564b12f16d3b2424390))

### [1.2.4](https://github.com/markwoon/NationsTournamentTool/compare/v1.2.3...v1.2.4) (2021-04-13)


### Bug Fixes

* fix GH actions ([01677ff](https://github.com/markwoon/NationsTournamentTool/commit/01677ffbe9fc833501a564f44493a923279af762))

### [1.2.3](https://github.com/markwoon/NationsTournamentTool/compare/v1.2.2...v1.2.3) (2021-04-13)


### Bug Fixes

* cleanup ([bbb512f](https://github.com/markwoon/NationsTournamentTool/commit/bbb512f60745aacc4a7a7846be19cab97de279e5))

### [1.2.2](https://github.com/markwoon/NationsTournamentTool/compare/v1.2.1...v1.2.2) (2021-04-13)


### Bug Fixes

* remove [skip ci] to trigger release ([5e7a73f](https://github.com/markwoon/NationsTournamentTool/commit/5e7a73f6afb70e39f092a573442b37f6644cebb8))

### [1.2.1](https://github.com/markwoon/NationsTournamentTool/compare/v1.2.0...v1.2.1) (2021-04-13)


### Bug Fixes

* add game start/finish time to improve slowest player calculations ([546d0e2](https://github.com/markwoon/NationsTournamentTool/commit/546d0e2bb0dc33274e86eaa0fa2b6c5ba6aeb86b))
* show app version in title ([df14fa6](https://github.com/markwoon/NationsTournamentTool/commit/df14fa642ad7c548fb5e444932952fda3d300fc7))

## [1.2.0](https://github.com/markwoon/NationsTournamentTool/compare/v1.1.2...v1.2.0) (2021-03-12)


### Features

* support toggling normal/final score mode ([8970c8e](https://github.com/markwoon/NationsTournamentTool/commit/8970c8e33e33ce8093008478e997f4aeafcb87a1))


### Bug Fixes

* add score mode help text ([8d32c48](https://github.com/markwoon/NationsTournamentTool/commit/8d32c480d397039bb9603cc961cb5981befa209f))

### [1.1.2](https://github.com/markwoon/NationsTournamentTool/compare/v1.1.1...v1.1.2) (2021-03-11)


### Bug Fixes

* fix release GH action, take 2 [skip ci] ([3ba49a4](https://github.com/markwoon/NationsTournamentTool/commit/3ba49a4ec389bd2c32a3c4a9d9b7ad883a261691))

### [1.1.1](https://github.com/markwoon/NationsTournamentTool/compare/v1.1.0...v1.1.1) (2021-03-11)


### Bug Fixes

* fix release GH action ([283088a](https://github.com/markwoon/NationsTournamentTool/commit/283088a194987871a838b80c3b6b02e621d73881))

## [1.1.0](https://github.com/markwoon/NationsTournamentTool/compare/v1.0.2...v1.1.0) (2021-03-11)


### Features

* calculate tournament points ([2862717](https://github.com/markwoon/NationsTournamentTool/commit/28627171e189761b70dc4be2571c8dd0160b7da2))
* support re-reading game info file ([7613125](https://github.com/markwoon/NationsTournamentTool/commit/7613125dd2a439a59a16ab15f5611a288a47ccbd))


### Bug Fixes

* fix scoring, add support for scoring unfinished matches ([8b2a1c6](https://github.com/markwoon/NationsTournamentTool/commit/8b2a1c60e0003643d04720e917914393cda89de7))
* update last updated timestamp ([3ab406c](https://github.com/markwoon/NationsTournamentTool/commit/3ab406cd71ff76706943471715df955bfa2df982))

### [1.0.2](https://github.com/markwoon/NationsTournamentTool/compare/v1.0.1...v1.0.2) (2021-03-01)


### Bug Fixes

* rally support updating app version with semantic-release ([d72ff1f](https://github.com/markwoon/NationsTournamentTool/commit/d72ff1fbcd25be01cbf7982577323cf1e3585d6b))

### [1.0.1](https://github.com/markwoon/NationsTournamentTool/compare/v1.0.0...v1.0.1) (2021-03-01)


### Bug Fixes

* support updating app version with semantic-release ([88f7fd1](https://github.com/markwoon/NationsTournamentTool/commit/88f7fd1b3caba429697b35b36d286d31a3483998))

## 1.0.0 (2021-03-01)


### Features

* initial commit base app ([099f9e6](https://github.com/markwoon/NationsTournamentTool/commit/099f9e6eb392ae157e4fbf5f2e0a03c412efa81f))
