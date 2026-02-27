import os
import json
from fastapi import FastAPI
from pydantic import BaseModel
from openai import OpenAI

app = FastAPI()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

class TaskRequest(BaseModel):
    goal: str

def write_file(filename: str, content: str):
    with open(filename, "w") as f:
        f.write(content)
    return f"File {filename} written successfully."

@app.post("/run")
def run_agent(task: TaskRequest):
    tools = [
        {
            "type": "function",
            "function": {
                "name": "write_file",
                "description": "Write content to a file",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "filename": {"type": "string"},
                        "content": {"type": "string"}
                    },
                    "required": ["filename", "content"]
                }
            }
        }
    ]

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": "You are an autonomous agent that completes goals using tools."},
            {"role": "user", "content": task.goal}
        ],
        tools=tools
    )

    message = response.choices[0].message

    if message.tool_calls:
        tool_call = message.tool_calls[0]
        args = json.loads(tool_call.function.arguments)

        result = write_file(**args)

        return {"status": "completed", "result": result}

    return {"status": "no_action", "message": message.content}
