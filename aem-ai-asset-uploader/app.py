import os
import base64
import requests
from openai import OpenAI

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
AEM_AUTHOR_URL = os.getenv("AEM_AUTHOR_URL")
AEM_USERNAME = os.getenv("AEM_USERNAME")
AEM_PASSWORD = os.getenv("AEM_PASSWORD")
DAM_PATH = os.getenv("DAM_PATH", "/content/dam/generated")

client = OpenAI(api_key=OPENAI_API_KEY)


def generate_image(prompt):
    result = client.images.generate(
        model="gpt-image-1",
        prompt=prompt,
        size="1024x1024"
    )

    image_base64 = result.data[0].b64_json
    image_bytes = base64.b64decode(image_base64)

    filename = "generated-image.png"

    with open(filename, "wb") as f:
        f.write(image_bytes)

    return filename


def upload_to_aem(file_path):
    upload_url = f"{AEM_AUTHOR_URL}{DAM_PATH}.createasset.html"

    with open(file_path, "rb") as f:
        files = {
            "file": (file_path, f, "image/png")
        }

        response = requests.post(
            upload_url,
            auth=(AEM_USERNAME, AEM_PASSWORD),
            files=files
        )

    if response.status_code not in [200, 201]:
        raise Exception(f"AEM upload failed: {response.text}")

    print("Upload successful!")


def run_agent(goal):
    print("Generating image...")
    image_path = generate_image(goal)

    print("Uploading to AEM...")
    upload_to_aem(image_path)

    print("Done!")


if __name__ == "__main__":
    goal = os.getenv("GOAL", "A futuristic smart city at sunset")
    run_agent(goal)