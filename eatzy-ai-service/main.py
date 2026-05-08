from fastapi import FastAPI, HTTPException, Request
import urllib.parse
from fastapi.middleware.cors import CORSMiddleware
import py_eureka_client.eureka_client as eureka_client
from dotenv import load_dotenv
import os
import uvicorn
from google import genai
from pydantic import BaseModel
import requests

# Load environment variables
load_dotenv()

app = FastAPI(title="Eatzy AI Service")

# Allow CORS for testing HTML interface
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

import asyncio

# Setup Eureka
EUREKA_SERVER = os.getenv("EUREKA_SERVER_URL", "http://localhost:8761/eureka")
APP_NAME = "eatzy-ai-service"
INSTANCE_PORT = int(os.getenv("SERVICE_PORT", 8089))
INSTANCE_HOST = os.getenv("INSTANCE_HOST", "localhost")
API_GATEWAY_URL = os.getenv("API_GATEWAY_URL", "http://localhost:8080")

@app.on_event("startup")
async def startup_event():
    # Register with Eureka (with retry)
    max_retries = 10
    for i in range(max_retries):
        try:
            await eureka_client.init_async(
                eureka_server=EUREKA_SERVER,
                app_name=APP_NAME,
                instance_port=INSTANCE_PORT,
                instance_host=INSTANCE_HOST
            )
            print(f"Successfully registered {APP_NAME} with Eureka on port {INSTANCE_PORT}")
            break
        except Exception as e:
            print(f"Failed to register with Eureka (attempt {i+1}/{max_retries}): {e}")
            await asyncio.sleep(5)

    # Test Gemini Connection
    try:
        # We will initialize ChromaDB and other LangChain components here later
        api_key = os.getenv("GEMINI_API_KEY")
        if not api_key:
            print("WARNING: GEMINI_API_KEY is not set.")
        else:
            print("Gemini API Key loaded successfully.")
    except Exception as e:
        print(f"Failed to initialize AI components: {e}")

@app.get("/api/v1/ai/health")
def health_check():
    return {"status": "UP"}

@app.get("/api/v1/ai/models")
def list_models():
    """List available Gemini models for current API key"""
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise HTTPException(status_code=500, detail="Missing API Key")
    try:
        client = genai.Client(api_key=api_key)
        models = client.models.list()
        return {"models": [m.name for m in models]}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

class ReviewInsightsRequest(BaseModel):
    pass

@app.post("/api/v1/ai/restaurants/{restaurant_id}/insights")
async def get_restaurant_insights(restaurant_id: str, request: Request):
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise HTTPException(status_code=500, detail="AI Service is not configured properly (Missing API Key)")
        
    # Lấy thông tin Authorization header từ request gốc (nếu có)
    headers = {}
    auth_header = request.headers.get("Authorization")
    if auth_header:
        headers["Authorization"] = auth_header

    gateway_url = API_GATEWAY_URL
        
    try:
        # 1. Gọi API lấy thông tin nhà hàng qua API Gateway
        rest_res = requests.get(f"{gateway_url}/api/v1/restaurants/{restaurant_id}", headers=headers)
        if rest_res.status_code != 200:
            raise HTTPException(status_code=404, detail=f"Restaurant not found or error: {rest_res.text}")
        
        rest_data = rest_res.json()
        
        # Spring Boot trả về qua ResponseEntity, có thể chứa trực tiếp data hoặc bọc trong 'data'/'result'
        if "data" in rest_data:
            rest_data = rest_data["data"]
        elif "result" in rest_data:
            rest_data = rest_data["result"]
            
        restaurant_name = rest_data.get("name")
        if not restaurant_name:
            raise HTTPException(status_code=404, detail="Could not extract restaurant name")

        # 2. Gọi API lấy danh sách reviews của nhà hàng
        encoded_name = urllib.parse.quote(restaurant_name)
        rev_res = requests.get(f"{gateway_url}/api/v1/reviews/target?reviewTarget=restaurant&targetName={encoded_name}", headers=headers)
        if rev_res.status_code != 200:
            raise HTTPException(status_code=500, detail=f"Failed to fetch reviews: {rev_res.text}")
            
        rev_data = rev_res.json()
        
        reviews_list = []
        if "result" in rev_data:
            reviews_list = rev_data["result"]
        elif "data" in rev_data:
            reviews_list = rev_data["data"]
        elif isinstance(rev_data, list):
            reviews_list = rev_data
            
        if not reviews_list or len(reviews_list) == 0:
            return {
                "restaurant_id": restaurant_id,
                "restaurant_name": restaurant_name,
                "insights": "Chưa có đánh giá nào cho nhà hàng này để AI phân tích."
            }
            
        # Trích xuất nội dung comment
        real_reviews = []
        for rev in reviews_list:
            comment = rev.get("comment")
            if comment and str(comment).strip() != "":
                real_reviews.append(comment)

        if not real_reviews:
            return {
                "restaurant_id": restaurant_id,
                "restaurant_name": restaurant_name,
                "insights": "Các đánh giá hiện tại không có nội dung chữ để AI phân tích (chỉ có số sao)."
            }
        
        # 3. Process with Gemini
        client = genai.Client(api_key=api_key)
        reviews_text = "\n".join([f"- {r}" for r in real_reviews])
        
        prompt = f"""
        Bạn là một chuyên gia phân tích ẩm thực và tư vấn nhà hàng.
        Hãy đọc các đánh giá sau của khách hàng về nhà hàng "{restaurant_name}":
        
        {reviews_text}
        
        Dựa vào các đánh giá trên, hãy cung cấp cho chủ nhà hàng:
        1. Tóm tắt chung về cảm nhận của khách hàng.
        2. Các điểm mạnh.
        3. Các điểm cần cải thiện (vấn đề hương vị, dịch vụ, đóng gói).
        4. Hành động đề xuất (Actionable insights).
        
        Vui lòng trả về kết quả dưới định dạng dễ đọc.
        """
        
        response = client.models.generate_content(
            model="gemini-2.5-flash",
            contents=prompt
        )
        
        return {
            "restaurant_id": restaurant_id,
            "restaurant_name": restaurant_name,
            "total_reviews_analyzed": len(real_reviews),
            "insights": response.text
        }
        
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to generate insights: {str(e)}")

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=INSTANCE_PORT, reload=True)
