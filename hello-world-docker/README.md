# Hello Agent API

A minimal agentic AI microservice built with FastAPI and OpenAI.

This service exposes a `/run` endpoint that accepts a goal and allows an AI agent to use a tool (`write_file`) to complete the task.

The project demonstrates:

- Tool calling with OpenAI
- A simple agent pattern
- FastAPI backend
- Dockerized deployment

---

## 🚀 Features

- Accepts a goal via REST API
- Uses OpenAI tool calling
- Executes a local function (`write_file`)
- Returns structured JSON response
- Fully containerized with Docker

---

## 🏗 Architecture

The agent follows a simple pattern:

1. Receive a goal
2. Provide tool definitions to the model
3. Let the model decide whether to call a tool
4. Execute the tool if requested
5. Return the result

This is a minimal implementation of an agent loop.

---

## 📂 Project Structure

```
.
├── app.py
├── Dockerfile
├── requirements.txt
└── README.md
```

---

## 🔑 Prerequisites

- Docker installed
- An OpenAI API key
- Billing enabled at https://platform.openai.com

---

## 🔐 Setting Your API Key

You must provide your OpenAI API key via environment variable.

Mac/Linux:

```bash
export OPENAI_API_KEY=your_key_here
```

Windows (PowerShell):

```
setx OPENAI_API_KEY "your_key_here"
```
Or pass it directly to Docker when running the container.

---

## 🐳 Build the Docker Image

From the project root:

```
docker build -t hello-agent .
```
▶️ Run the Container
```
docker run -p 8000:8000 -e OPENAI_API_KEY=<your_api_key> hello-agent
```

The API will be available at:

http://localhost:8000

---

## 🧪 Test the Agent

Send a POST request:

```
curl -X POST http://localhost:8000/run \
  -H "Content-Type: application/json" \
  -d '{"goal": "Write Hello World into a file called hello.txt"}'
```
Expected response:
```
{
  "status": "completed",
  "result": "File hello.txt written successfully."
}
```

---

## 📡 API Endpoint
```
POST /run
```
```
Request Body
{
  "goal": "Write Hello World into a file called hello.txt"
}
```
Successful Response
```
{
  "status": "completed",
  "result": "File hello.txt written successfully."
}
```
If No Tool Is Used
```
{
  "status": "no_action",
  "message": "Model response"
}
```

---

## 🧠 How It Works

The service defines a single tool:

```
write_file(filename: str, content: str)
```

The OpenAI model decides whether to call this function.
If it does, the function executes and the result is returned.

This demonstrates a basic agent pattern with:

- Tool definition

- Tool selection by model

- Tool execution

- Result handling

---

## 📦 Persisting Files (Optional)

By default, files are written inside the container and are removed when the container stops.

To persist files, mount a volume:

```
docker run -p 8000:8000 \
  -v $(pwd)/data:/app \
  -e OPENAI_API_KEY=<your_api_key> \
  hello-agent
```

Files will then be stored in the local data/ directory.

---

## 🛠 Potential Improvements

- Add multi-step agent loop

- Add structured logging and error handling

- Add authentication

- Add persistent storage service

- Add async tool execution

- Add health check endpoint

- Deploy to Kubernetes

- Add CI/CD pipeline

---

## 📜 License

For educational and experimentation purposes.

