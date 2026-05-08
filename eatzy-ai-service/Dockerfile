# Sử dụng official Python runtime làm base image
FROM python:3.12-slim

# Thiết lập thư mục làm việc trong container
WORKDIR /app

# Copy file requirements.txt vào container
COPY requirements.txt .

# Cài đặt các thư viện cần thiết
RUN pip install --no-cache-dir -r requirements.txt

# Copy toàn bộ source code vào container
COPY . .

# Mở cổng 8089 (theo cấu hình của service)
EXPOSE 8089

# Lệnh chạy ứng dụng khi container khởi động
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8089"]
