<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NEXO NETWORK - Đặt Lại Mật Khẩu</title>
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
        .form-group {
            margin-bottom: 25px;
            text-align: left;
        }
        label {
            display: block;
            color: #333;
            font-weight: 500;
            margin-bottom: 8px;
            font-size: 14px;
        }
        input[type="password"] {
            width: 100%;
            padding: 12px 16px;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            font-size: 15px;
            transition: border-color 0.3s ease;
            outline: none;
        }
        input[type="password"]:focus {
            border-color: #007bff;
        }
        .error-message {
            background-color: #fee;
            color: #c33;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            font-size: 14px;
            border-left: 4px solid #c33;
            text-align: left;
        }
        .success-message {
            background-color: #d4edda;
            color: #155724;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            font-size: 14px;
            border-left: 4px solid #28a745;
            text-align: left;
        }
        .warning-message {
            background-color: #fff3cd;
            color: #856404;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            font-size: 14px;
            border-left: 4px solid #ffc107;
            text-align: left;
        }
        .info-message {
            background-color: #d1ecf1;
            color: #0c5460;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            font-size: 14px;
            border-left: 4px solid #17a2b8;
            text-align: left;
        }
        .btn {
            display: inline-block;
            width: 100%;
            background-color: #007bff;
            color: white;
            padding: 14px 40px;
            text-decoration: none;
            border: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s ease;
            box-shadow: 0 4px 15px rgba(0,123,255,0.3);
        }
        .btn:hover {
            background-color: #0056b3;
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(0,123,255,0.4);
        }
        .btn:disabled {
            background-color: #ccc;
            cursor: not-allowed;
            transform: none;
        }
        .back-link {
            display: block;
            margin-top: 20px;
            color: #007bff;
            text-decoration: none;
            font-size: 14px;
        }
        .back-link:hover {
            text-decoration: underline;
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
            <#-- <img src="${url.resourcesPath}/img/logo.png" alt="NEXO NETWORK"> -->
            <h1>NEXO NETWORK</h1>
        </div>
        
        <h2>Đặt Lại Mật Khẩu</h2>
        
        <div class="message">
            <p>Vui lòng nhập mật khẩu mới cho tài khoản của bạn.</p>
        </div>
        
        <#-- Hiển thị message lỗi validation -->
        <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
            <#if message.type == 'success'>
                <div class="success-message">
                    ✓ Mật khẩu của bạn đã được cập nhật thành công.
                </div>
            <#elseif message.type == 'error'>
                <div class="error-message">
                    <#if message.summary?contains('Password confirmation') || message.summary?contains('passwords don\'t match')>
                        ✕ Xác nhận mật khẩu không khớp. Vui lòng thử lại.
                    <#elseif message.summary?contains('Invalid password') || message.summary?contains('policy')>
                        ✕ Mật khẩu không đáp ứng yêu cầu bảo mật. Vui lòng chọn mật khẩu mạnh hơn.
                    <#else>
                        ✕ ${message.summary}
                    </#if>
                </div>
            </#if>
        </#if>
        
        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">
            <input type="text" id="username" name="username" value="${username}" autocomplete="username" readonly="readonly" style="display:none;">
            <input type="password" id="password" name="password" autocomplete="current-password" style="display:none;">
            
            <div class="form-group">
                <label for="password-new">Mật Khẩu Mới</label>
                <input type="password" id="password-new" name="password-new" autofocus autocomplete="new-password" required />
            </div>

            <div class="form-group">
                <label for="password-confirm">Xác Nhận Mật Khẩu</label>
                <input type="password" id="password-confirm" name="password-confirm" autocomplete="new-password" required />
            </div>

            <div class="form-group">
                <#if isAppInitiatedAction??>
                    <button type="submit" class="btn" name="login" id="kc-submit">Xác Nhận</button>
                    <button type="submit" class="btn" name="cancel-aia" value="true" style="background-color: #6c757d; margin-top: 10px;">Hủy Bỏ</button>
                <#else>
                    <button type="submit" class="btn" name="login" id="kc-submit">Xác Nhận</button>
                </#if>
            </div>
        </form>
        
    </div>
</body>
</html>
