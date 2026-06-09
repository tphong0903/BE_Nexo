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
EMAIL="tanvinh58@email.com"
STAGING=0                  # Đặt 1 để test (không bị rate limit), 0 để lấy cert thật
# -------------------------------------------

# Chỉ cấp cert cho các subdomain trỏ về VPS này
# nexosocial.id.vn (root) trỏ server khác nên KHÔNG thêm vào
DOMAINS="-d api.${DOMAIN} -d auth.${DOMAIN}"

echo "=== [1/4] Tạo thư mục certbot ==="
mkdir -p ./certbot/www/.well-known/acme-challenge
mkdir -p ./certbot/conf

echo "=== [2/4] Khởi động nginx tạm (HTTP only) ==="

# Dọn container cũ nếu còn sót
docker rm -f nginx-init 2>/dev/null || true

# Nếu nginx reverse proxy chính đang chạy, tắt nó tạm thời để giải phóng port 80
# compose của dự án dùng container_name=nginx
docker rm -f nginx 2>/dev/null || true

# Viết config nginx tạm ra /tmp (không phụ thuộc file trong repo)
cat > /tmp/nginx-certbot-init.conf << 'EOF'
server {
    listen 80;
    server_name api.nexosocial.id.vn auth.nexosocial.id.vn;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 200 'OK';
        add_header Content-Type text/plain;
    }
}
EOF

# Dùng docker run (KHÔNG phải docker compose run) để chạy nginx độc lập
docker run -d \
  --name nginx-init \
  -v "/tmp/nginx-certbot-init.conf:/etc/nginx/conf.d/default.conf:ro" \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  -p "80:80" \
  nginx:alpine

sleep 3

# Kiểm tra nginx có chạy không
if ! docker ps --format '{{.Names}}' | grep -q "^nginx-init$"; then
  echo "❌ Nginx init thất bại! Kiểm tra logs:"
  docker logs nginx-init
  exit 1
fi
echo "✅ Nginx tạm đang chạy trên port 80"

echo "=== [3/4] Lấy certificate từ Let's Encrypt ==="

STAGING_ARG=""
if [ "$STAGING" = "1" ]; then
  STAGING_ARG="--staging"
  echo "    ⚠️  STAGING MODE - cert test, không dùng được thật"
fi

docker run --rm \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
  certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --cert-name ${DOMAIN} \
    $DOMAINS \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    --keep-until-expiring \
    $STAGING_ARG

echo "=== [4/4] Dọn dẹp nginx tạm ==="
docker rm -f nginx-init 2>/dev/null || true

echo ""
echo "======================================================"
echo "  ✅ XONG! Certificate đã lưu tại ./certbot/conf"
echo ""
echo "  Chạy toàn bộ stack:"
echo "  docker compose -f docker-compose.prod.yml up -d"
echo "======================================================"
