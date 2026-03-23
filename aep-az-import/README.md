# AEP AzCopy Import

A Docker-based tool for uploading CSV files to the [Adobe Experience Platform (AEP) Data Landing Zone](https://experienceleague.adobe.com/en/docs/platform-learn/tutorial-one-adobe/activation/dc/dc12/ex5#copy-your-csv-file-to-your-aep-data-landing-zone) using [AzCopy v10](https://docs.microsoft.com/en-us/azure/storage/common/storage-use-azcopy-v10).

Supports both single-file and batch uploads. All output is streamed to `docker logs` and saved as timestamped log files.

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) with Apple Silicon (ARM64) support
- A valid SAS URI from your AEP Data Landing Zone

---

## Getting started

### 1. Get your SAS URI from AEP

1. Go to **AEP > Sources > Data Landing Zone**
2. Click **View credentials**
3. Copy the **SAS URI** (it looks like `https://<account>.blob.core.windows.net/<container>?sv=...&sig=...`)

### 2. Configure credentials

```bash
cp .env.example .env
```

Open `.env` and paste your SAS URI:

```env
AZCOPY_SAS_URI=https://your-storage-account.blob.core.windows.net/dlz-user-container?sv=...&sig=...
```

The SAS URI is composed of three parts:

```
https://{storageAccountName}.blob.core.windows.net/{containerName}?{sasToken}
```

| Part | Example |
|---|---|
| `storageAccountName` | `sndbxdtlnd2bimpjpzo14hp6` |
| `containerName` | `dlz-user-container` |
| `sasToken` | `sv=2020-04-08&si=dlz-...&sr=c&sp=racwdlm&sig=...` |

> **Note:** SAS URIs expire. When you get a new one from AEP, just update this value — no rebuild needed.

### 3. Build the Docker image

Only needed once, or after changes to the `Dockerfile`:

```bash
docker compose build
```

---

## Usage

Drop your CSV file(s) into the `./data` directory, then run one of the commands below.

### Upload a single file

```bash
docker compose run azcopy <filename.csv>
```

Example:

```bash
docker compose run azcopy global-context-websiteinteractions.csv
```

### Upload all CSVs in ./data (batch mode)

```bash
docker compose run azcopy
```

---

## Logs

All output is written to **both**:

- `stdout` — visible via `docker logs <container-id>`
- `./logs/upload_<timestamp>.log` — persisted on your host machine

AzCopy's internal job logs are stored under `./logs/azcopy-jobs/`.

---

## Project structure

```
aep-az-import/
├── Dockerfile              # Builds image with AzCopy ARM64 binary
├── docker-compose.yml      # Service definition, volumes, and env config
├── scripts/
│   └── upload.sh           # Upload logic (single-file and batch modes)
├── data/                   # Place CSV files here before uploading
├── logs/                   # Upload logs are written here
├── .env                    # Your credentials (not committed to git)
└── .env.example            # Credentials template
```

---

## Security

- `.env` is listed in `.gitignore` and will never be committed
- CSV files in `./data` are also excluded from git
- Never share or commit your SAS URI — treat it like a password
