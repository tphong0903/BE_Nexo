#!/bin/bash
# =================================================================
# Script khởi tạo SSL Certificate (Let's Encrypt) lần đầu
# Chạy 1 LẦN DUY NHẤT trên server để lấy certificate
#
# Cách dùng:
#   chmod +x init-ssl.sh
#   ./init-ssl.sh
# =================================================================

set -e

# ---- CẤU HÌNH - THAY ĐỔI CHO PHÙ HỢP ----
DOMAIN="nexosocial.id.vn"
EMAIL="tanvinh58@email.com"          # <-- Đổi thành email thật của bạn
STAGING=0                       # Đặt 1 để test (không bị rate limit), 0 để lấy cert thật
# -------------------------------------------

DOMAINS="-d ${DOMAIN} -d api.${DOMAIN} -d auth.${DOMAIN}"

echo "=== [1/4] Tạo thư mục certbot ==="
mkdir -p ./certbot/www
mkdir -p ./certbot/conf

echo "=== [2/4] Khởi động nginx với config HTTP tạm (init) ==="
# Dùng nginx.init.conf (chỉ HTTP, không cần cert) để certbot có thể verify
docker compose -f docker-compose.prod.yml run --rm \
  -v "$(pwd)/nginx/nginx.init.conf:/etc/nginx/conf.d/default.conf:ro" \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  -p "80:80" \
  --name nginx-init \
  nginx:alpine nginx -g "daemon off;" &

NGINX_PID=$!
sleep 3

echo "=== [3/4] Lấy certificate từ Let's Encrypt ==="

STAGING_ARG=""
if [ "$STAGING" = "1" ]; then
  STAGING_ARG="--staging"
  echo "    [STAGING MODE - cert test, không dùng được thật]"
fi

docker run --rm \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
  certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    $DOMAINS \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    $STAGING_ARG

echo "=== [4/4] Dừng nginx tạm ==="
kill $NGINX_PID 2>/dev/null || true
docker rm -f nginx-init 2>/dev/null || true

echo ""
echo "======================================================"
echo "  XONG! Certificate đã được lưu tại ./certbot/conf"
echo ""
echo "  Bây giờ chạy toàn bộ stack:"
echo "  docker compose -f docker-compose.prod.yml up -d"
echo "======================================================"
