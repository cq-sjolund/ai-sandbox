# AEM AI Asset Uploader

An AI-powered agent that:

Generates an image using OpenAI based on a goal prompt

Uploads the generated image automatically to an AEM Author environment (DAM)

Runs fully containerized via Docker

## 📦 Features

- 🧠 OpenAI image generation (gpt-image-1)

- 📤 Automatic upload to AEM DAM

- 🔐 Environment-based configuration

- 🐳 Docker-ready

- ⚙️ Easily extendable to a microservice

## 📂 Project Structure
```
.
├── app.py
├── Dockerfile
├── requirements.txt
├── .env
└── README.md
```

## 🚀 How It Works
```
Goal Prompt
   ↓
OpenAI Image Generation
   ↓
Image Saved Locally
   ↓
Upload to AEM Author DAM
   ↓
Asset Available in AEM
```

## 🔑 Prerequisites

- Python 3.11+
- Docker (optional but recommended)
- OpenAI API key
- AEM Author access (6.5 or Cloud)
- DAM folder path (e.g. /content/dam/my-project/generated)

## 🔐 Environment Configuration

Create a .env file:

OPENAI_API_KEY=sk-xxxx
AEM_AUTHOR_URL=https://author.mycompany.com
AEM_USERNAME=admin
AEM_PASSWORD=admin
DAM_PATH=/content/dam/my-project/generated
GOAL=A modern eco-friendly office building, ultra realistic

⚠️ Do NOT commit .env to version control.

## 🧪 Run Locally (Without Docker)

Install dependencies:
```
pip install -r requirements.txt
```
Run:
```
python app.py
```
Override goal:

GOAL="Luxury Iceland landscape hero image" python app.py

## 🐳 Run with Docker
Build
```
docker build -t ai-aem-agent .
```
Run

```
docker run --env-file .env ai-aem-agent
```

Override goal dynamically:

```
docker run --env-file .env \
  -e GOAL="Futuristic smart city at sunset" \
  ai-aem-agent
```

## 🏢 AEM Compatibility
### AEM 6.5

- Uses .createasset.html

- Basic Auth supported

### AEM as a Cloud Service

For production usage you should:

- Use Direct Binary Upload API

- Use Adobe IMS OAuth

- Avoid Basic Auth

## 🔧 Configuration Variables
Variable	Description
OPENAI_API_KEY	Your OpenAI API key
AEM_AUTHOR_URL	AEM author base URL
AEM_USERNAME	AEM username
AEM_PASSWORD	AEM password
DAM_PATH	Target DAM folder
GOAL	Image generation prompt

## 🧱 Architecture Overview
```
+----------------+
|    Docker      |
|  AI Agent App  |
+--------+-------+
         |
         v
+----------------+
|    OpenAI API  |
+----------------+
         |
         v
+----------------+
|   AEM Author   |
|   DAM Upload   |
+----------------+
```

## 📈 Future Improvements

- Convert to FastAPI microservice

- Add metadata tagging (title, alt text, tags)

- Smart file naming

- Retry logic & exponential backoff

- Structured logging

- Health endpoint

- OAuth for AEM Cloud

- Kubernetes deployment

## 🔒 Security Notes

- Never store credentials in source code

- Use environment variables

- Use IMS OAuth for AEM Cloud production

- Consider secret managers (Vault, AWS Secrets Manager, etc.)

## 📜 License

For educational and experimentation purposes.