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
        .btn-secondary {
            background-color: #6c757d;
            box-shadow: 0 4px 15px rgba(108,117,125,0.3);
            margin-top: 15px;
        }
        .btn-secondary:hover {
            background-color: #545b62;
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
        .hidden { display: none; }
        @media (max-width: 600px) {
            .container { padding: 40px 30px; }
            .logo h1 { font-size: 26px; }
            h2 { font-size: 20px; }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">
            <h1>NEXO NETWORK</h1>
        </div>
        
        <#if message?has_content>
            <#if message.type == 'error'>
                <div class="error-message">
                    <#if message.summary?contains('expired') || message.summary?contains('hết hạn')>
                        ✕ <strong>Link đã hết hạn</strong><br/>
                        Link của bạn đã hết hạn. Vui lòng yêu cầu link mới.
                    <#elseif message.summary?contains('invalid') || message.summary?contains('không hợp lệ')>
                        ✕ <strong>Link không hợp lệ</strong><br/>
                        Link không hợp lệ hoặc đã được sử dụng. Vui lòng yêu cầu link mới.
                    <#else>
                        ✕ ${message.summary}
                    </#if>
                </div>
            </#if>
        </#if>

        <#-- Trạng thái ban đầu: hiện nút hành động -->
        <div id="action-state">
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
                <#if message?has_content && message.type == 'error'>
                    <a href="${client.rootUrl!'https://nexosocial.id.vn'}/auth/login" class="btn">« Quay Lại Đăng Nhập</a>
                <#elseif actionUri?has_content>
                    <#if requiredActions??>
                        <#if requiredActions?seq_contains("UPDATE_PASSWORD")>
                            <a href="${actionUri}" class="btn">Đặt Lại Mật Khẩu</a>
                        <#elseif requiredActions?seq_contains("VERIFY_EMAIL")>
                            <a href="#" class="btn" id="verifyBtn" onclick="handleVerify(event)">Xác Minh Email</a>
                        <#elseif requiredActions?seq_contains("UPDATE_PROFILE")>
                            <a href="${actionUri}" class="btn">Cập Nhật Thông Tin</a>
                        <#else>
                            <a href="${actionUri}" class="btn">Xác Nhận</a>
                        </#if>
                    <#else>
                        <a href="#" class="btn" id="verifyBtn" onclick="handleVerify(event)">Xác Minh Email</a>
                    </#if>
                    <br/>
                    <a href="${client.rootUrl!'https://nexosocial.id.vn'}/auth/login" class="btn btn-secondary">« Quay Lại Đăng Nhập</a>
                <#elseif pageRedirectUri?has_content>
                    <a href="${client.rootUrl!'https://nexosocial.id.vn'}/auth/login" class="btn">« Quay Lại Ứng Dụng</a>
                <#elseif client?? && client.baseUrl?has_content>
                    <a href="${client.rootUrl!'https://nexosocial.id.vn'}/auth/login" class="btn">« Quay Lại Ứng Dụng</a>
                <#else>
                    <a href="https://nexosocial.id.vn/auth/login" class="btn">« Quay Lại Đăng Nhập</a>
                </#if>
            </#if>
        </div>

        <#-- Trạng thái sau khi verify thành công -->
        <div id="success-state" class="hidden">
            <div class="success-message">
                ✓ <strong>Xác minh email thành công!</strong><br/>
                Tài khoản của bạn đã được kích hoạt. Đang chuyển về trang đăng nhập...
            </div>
            <a href="${client.rootUrl!'https://nexosocial.id.vn'}/auth/login" class="btn">Đăng Nhập Ngay</a>
        </div>
    </div>

    <script>
    function handleVerify(e) {
        e.preventDefault();
        var actionUrl = '${actionUri!""}';
        var loginUrl = '${client.rootUrl!"https://nexosocial.id.vn"}/auth/login';

        if (!actionUrl) {
            alert('Không có liên kết xác minh!');
            return;
        }

        // Gọi actionUri bằng fetch để Keycloak xử lý verify ở background
        // Sau đó hiện thông báo thành công + redirect về login
        fetch(actionUrl, { method: 'GET', redirect: 'follow', credentials: 'include' })
            .then(function(response) {
                // Keycloak trả về HTML page (có thể 200 hoặc redirect),
                // dù thế nào thì verify đã được xử lý
                document.getElementById('action-state').classList.add('hidden');
                document.getElementById('success-state').classList.remove('hidden');
                // Tự động redirect sau 2 giây
                setTimeout(function() {
                    window.location.href = loginUrl;
                }, 2000);
            })
            .catch(function(err) {
                // Nếu fetch lỗi (CORS, network), fallback: mở actionUri trực tiếp
                // rồi redirect sau vài giây
                window.location.href = actionUrl;
            });
    }
    </script>
</body>
</html>
