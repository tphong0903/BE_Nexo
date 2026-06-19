<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NEXO NETWORK - Lỗi Xác Nhận</title>
    <script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            display: flex; justify-content: center; align-items: center;
            min-height: 100vh; padding: 20px;
        }
        .container {
            background: #fff; padding: 50px 60px; border-radius: 16px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2); text-align: center;
            max-width: 500px; width: 100%; animation: fadeIn 0.5s ease-in;
        }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } }
        .logo { margin-bottom: 30px; }
        .logo h1 { color: #007bff; font-size: 32px; font-weight: 700; letter-spacing: 2px; margin: 0; }
        h2 { color: #333; margin-bottom: 20px; font-size: 24px; font-weight: 600; }
        .message { color: #555; margin-bottom: 35px; font-size: 16px; line-height: 1.6; }
        .btn {
            border: none; cursor: pointer; display: inline-block;
            background-color: #007bff; color: white; padding: 14px 40px;
            text-decoration: none; border-radius: 8px; font-size: 16px;
            font-weight: 600; transition: all 0.3s ease; box-shadow: 0 4px 15px rgba(0,123,255,0.3);
            margin-bottom: 15px; width: 100%;
        }
        .btn:hover { background-color: #0056b3; transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,123,255,0.4); }
        .btn-secondary {
            background-color: #6c757d; box-shadow: 0 4px 15px rgba(108,117,125,0.3);
        }
        .btn-secondary:hover { background-color: #5a6268; box-shadow: 0 6px 20px rgba(108,117,125,0.4); }
        .error-message {
            background-color: #fee; color: #c33; padding: 15px 20px;
            border-radius: 8px; margin-bottom: 25px; font-size: 15px;
            border-left: 4px solid #c33; text-align: left;
        }
        @media (max-width: 600px) { .container { padding: 40px 30px; } .logo h1 { font-size: 26px; } h2 { font-size: 20px; } }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">
            <h1>NEXO NETWORK</h1>
        </div>
        
        <h2>Có Lỗi Xảy Ra</h2>

        <#if message?has_content>
            <div class="error-message">
                <#if message.summary?contains('expired') || message.summary?contains('hết hạn')>
                    ✕ <strong>Link đã hết hạn</strong><br/>
                    Liên kết xác nhận của bạn đã quá thời gian hiệu lực.
                <#elseif message.summary?contains('invalid') || message.summary?contains('không hợp lệ')>
                    ✕ <strong>Link không hợp lệ</strong><br/>
                    Liên kết xác nhận không hợp lệ hoặc đã được sử dụng.
                <#else>
                    ✕ ${message.summary?no_esc}
                </#if>
            </div>
        </#if>

        <div class="message">
            <p>Liên kết xác nhận của bạn có thể đã hết hạn hoặc không hợp lệ. Nếu đây là xác minh email, bạn có thể gửi lại mã xác minh mới.</p>
        </div>

        <button id="resendBtn" class="btn" style="display: none;">Gửi Lại Mã Xác Minh</button>
        <a href="${(realm.attributes.frontendUrl)!(client.baseUrl)!(client.rootUrl)!'http://localhost:3000'}/auth/login" class="btn btn-secondary">« Quay Lại Đăng Nhập</a>

        <script>
        document.addEventListener('DOMContentLoaded', function() {
            var resendBtn = document.getElementById('resendBtn');
            const urlParams = new URLSearchParams(window.location.search);
            const token = urlParams.get('key');
            let userId = '';
            
            // Lấy ID từ jwt token trên URL
            if (token) {
                try {
                    const base64Url = token.split('.')[1];
                    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
                    const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                    }).join(''));
                    const payload = JSON.parse(jsonPayload);
                    userId = payload.sub || payload.keycloakId || '';
                    if (userId) {
                        resendBtn.style.display = 'inline-block';
                    }
                } catch (e) {
                    console.error("Lỗi khi parse token", e);
                }
            }

            if (resendBtn) {
                resendBtn.addEventListener('click', function() {
                    if (userId) {
                        resendBtn.innerText = 'Đang gửi...';
                        resendBtn.disabled = true;
                        
                        fetch('https://api.nexosocial.id.vn/api/auth/resend-verify-email', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ "ID": userId })
                        })
                        .then(res => res.json())
                        .then(data => {
                            if(data.status === 200 || data.status === 'success') {
                                Swal.fire({ icon: 'success', title: 'Thành công', text: 'Mã xác minh mới đã được gửi đến email của bạn! Vui lòng kiểm tra hộp thư.' });
                            } else {
                                Swal.fire({ icon: 'error', title: 'Thất bại', text: 'Có lỗi xảy ra: ' + (data.message || 'Vui lòng thử lại sau.') });
                            }
                        })
                        .catch(err => {
                            Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Lỗi kết nối tới máy chủ!' });
                        })
                        .finally(() => {
                            resendBtn.innerText = 'Gửi Lại Mã Xác Minh';
                            resendBtn.disabled = false;
                        });
                    } else {
                        Swal.fire({ icon: 'warning', title: 'Cảnh báo', text: 'Không lấy được thông tin người dùng. Vui lòng quay lại đăng nhập và yêu cầu gửi lại.' });
                    }
                });
            }
        });
        </script>
    </div>
</body>
</html>
