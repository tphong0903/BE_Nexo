<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NEXO NETWORK - Xác Nhận</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 20px;
        }
        .container {
            background: #fff;
            padding: 50px 60px;
            border-radius: 16px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            text-align: center;
            max-width: 500px;
            width: 100%;
            animation: fadeIn 0.5s ease-in;
        }
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(-20px); }
            to { opacity: 1; transform: translateY(0); }
        }
       
        .logo {
            margin-bottom: 30px;
        }
        .logo h1 {
            color: #007bff;
            font-size: 32px;
            font-weight: 700;
            letter-spacing: 2px;
            margin: 0;
        }
        .logo img {
            max-width: 180px;
            height: auto;
            margin-bottom: 15px;
        }
        h2 {
            color: #333;
            margin-bottom: 20px;
            font-size: 24px;
            font-weight: 600;
        }
        .message {
            color: #555;
            margin-bottom: 35px;
            font-size: 16px;
            line-height: 1.6;
        }
        .message b {
            color: #007bff;
        }
        .btn {
            border: none;
            cursor: pointer;
            display: inline-block;
            background-color: #007bff;
            color: white;
            padding: 14px 40px;
            text-decoration: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 600;
            transition: all 0.3s ease;
            box-shadow: 0 4px 15px rgba(0,123,255,0.3);
        }
        .btn:hover {
            background-color: #0056b3;
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(0,123,255,0.4);
        }
        .error {
            color: #dc3545;
            font-size: 14px;
            margin-top: 20px;
        }
        .error-message {
            background-color: #fee;
            color: #c33;
            padding: 15px 20px;
            border-radius: 8px;
            margin-bottom: 25px;
            font-size: 15px;
            border-left: 4px solid #c33;
            text-align: left;
        }
        .success-message {
            background-color: #d4edda;
            color: #155724;
            padding: 15px 20px;
            border-radius: 8px;
            margin-bottom: 25px;
            font-size: 15px;
            border-left: 4px solid #28a745;
            text-align: left;
        }
        #debug-info {
            background: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 4px;
            padding: 10px;
            margin-top: 20px;
            font-size: 12px;
            text-align: left;
            max-height: 200px;
            overflow-y: auto;
        }
        @media (max-width: 600px) {
            .container {
                padding: 40px 30px;
            }
            .logo h1 {
                font-size: 26px;
            }
            h2 {
                font-size: 20px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">
            <#-- Nếu có logo, uncomment dòng dưới -->
            <#-- <img src="${url.resourcesPath}/img/logo.png" alt="NEXO NETWORK"> -->
            <h1>NEXO NETWORK</h1>
        </div>
        
        <#if message?has_content>
            <#if message.type == 'error'>
                <div class="error-message">
                    <#if message.summary?contains('expired') || message.summary?contains('hết hạn')>
                        ✕ <strong>Link đã hết hạn</strong><br/>
                        Link đặt lại mật khẩu của bạn đã hết hạn. Vui lòng yêu cầu link mới.
                    <#elseif message.summary?contains('invalid') || message.summary?contains('không hợp lệ')>
                        ✕ <strong>Link không hợp lệ</strong><br/>
                        Link đặt lại mật khẩu không hợp lệ hoặc đã được sử dụng. Vui lòng yêu cầu link mới.
                    <#else>
                        ✕ ${message.summary}
                    </#if>
                </div>
            </#if>
        </#if>

        <h2>
            <#if requiredActions??>
                <#if requiredActions?seq_contains("UPDATE_PASSWORD")>
                    Đặt Lại Mật Khẩu
                <#elseif requiredActions?seq_contains("VERIFY_EMAIL")>
                    Xác Minh Email
                <#elseif requiredActions?seq_contains("UPDATE_PROFILE")>
                    Cập Nhật Thông Tin
                <#else>
                    Xác Nhận Hành Động
                </#if>
            <#else>
                Xác Nhận Hành Động
            </#if>
        </h2>
        
        <#if !message?has_content || message.type != 'error'>
            <div class="message">
                <#if requiredActions??>
                    <#if requiredActions?seq_contains("UPDATE_PASSWORD")>
                        <p>Nhấn vào nút bên dưới để đặt lại mật khẩu của bạn.</p>
                    <#elseif requiredActions?seq_contains("VERIFY_EMAIL")>
                        <p>Vui lòng nhấn vào nút bên dưới để hoàn tất xác minh email của bạn.</p>
                    <#elseif requiredActions?seq_contains("UPDATE_PROFILE")>
                        <p>Nhấn vào nút bên dưới để cập nhật thông tin cá nhân của bạn.</p>
                    <#else>
                        <p>Vui lòng nhấn vào nút bên dưới để hoàn tất xác nhận.</p>
                    </#if>
                <#else>
                    <p>Vui lòng nhấn vào nút bên dưới để hoàn tất xác minh email của bạn.</p>
                </#if>
            </div>
        </#if>
        
        <#if skipLink??>
            <#-- Không hiển thị link -->
        <#else>
           <#-- Nếu có lỗi (token hết hạn/invalid), chỉ hiển thị nút quay lại -->
            <#if message?has_content && message.type == 'error'>
                <a href="${client.rootUrl!'http://localhost:3000'}/auth/login" class="btn">« Quay Lại Đăng Nhập</a>
            <#elseif actionUri?has_content>
                <#if requiredActions??>
                    <#if requiredActions?seq_contains("UPDATE_PASSWORD")>
                        <a href="${actionUri}" class="btn">Đặt Lại Mật Khẩu</a>
                    <#elseif requiredActions?seq_contains("VERIFY_EMAIL")>
                            <button id="verifyBtn" class="btn">Xác Minh Email</button>

                    <#elseif requiredActions?seq_contains("UPDATE_PROFILE")>
                        <a href="${actionUri}" class="btn">Cập Nhật Thông Tin</a>
                    <#else>
                        <a href="${actionUri}" class="btn">Xác Nhận</a>
                    </#if>
                <#else>
                        <button id="verifyBtn" class="btn">Xác Minh Email</button>

                </#if>
            <#elseif pageRedirectUri?has_content>
                <a href="${client.rootUrl!'http://localhost:3000'}/auth/login" class="btn">« Quay Lại Ứng Dụng</a>
            <#elseif client?? && client.baseUrl?has_content>
                <a href="${client.rootUrl!'http://localhost:3000'}/auth/login" class="btn">« Quay Lại Ứng Dụng</a>
            <#else>
                <p class="error">Không có liên kết xác nhận khả dụng.</p>
            </#if>
        </#if>

        <script>
        document.addEventListener('DOMContentLoaded', function() {
            var btn = document.getElementById('verifyBtn');
            if (btn) {
                btn.addEventListener('click', function() {
                    const urlParams = new URLSearchParams(window.location.search);
                    const token = urlParams.get('key');
                    let userId = '';
                    let userEmail = '';
                    if (token) {
                        try {
                            const base64Url = token.split('.')[1];
                            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
                            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                            }).join(''));
                            const payload = JSON.parse(jsonPayload);
                            userId = payload.sub || payload.keycloakId || '';
                            userEmail = payload.eml || payload.email || '';
                        } catch (e) {}
                    }
                    if (userId && userEmail) {
                        fetch('http://localhost:8080/api/auth/verify-email', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ email: userEmail, keycloakId: userId })
                        })
                        .then(res => res.json())
                        .then(data => {
                            window.location.href = '${client.rootUrl!"http://localhost:3000"}/auth/login';
                        })
                        .catch(err => {
                            alert('Lỗi xác thực!');
                        });
                    } else {
                        alert('Không lấy được thông tin người dùng từ token!');
                    }
                });
            }
        });
        </script>
    </div>

</body>
</html>