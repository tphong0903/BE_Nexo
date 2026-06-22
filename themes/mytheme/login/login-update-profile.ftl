<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NEXO NETWORK - Cập Nhật Thông Tin</title>
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
            max-width: 520px;
            width: 100%;
            animation: fadeIn 0.5s ease-in;
        }
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(-20px); }
            to { opacity: 1; transform: translateY(0); }
        }
        .logo {
            margin-bottom: 25px;
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
            margin-bottom: 8px;
            font-size: 22px;
            font-weight: 600;
        }
        .subtitle {
            color: #888;
            font-size: 14px;
            margin-bottom: 30px;
        }
        .form-group {
            margin-bottom: 20px;
            text-align: left;
        }
        label {
            display: block;
            color: #333;
            font-weight: 500;
            margin-bottom: 7px;
            font-size: 14px;
        }
        label .required {
            color: #e74c3c;
            margin-left: 3px;
        }
        input[type="text"],
        input[type="email"] {
            width: 100%;
            padding: 12px 16px;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            font-size: 15px;
            transition: border-color 0.3s ease, box-shadow 0.3s ease;
            outline: none;
            color: #333;
            background: #fafafa;
        }
        input[type="text"]:focus,
        input[type="email"]:focus {
            border-color: #007bff;
            background: #fff;
            box-shadow: 0 0 0 3px rgba(0,123,255,0.1);
        }
        input[readonly] {
            background: #f0f0f0;
            color: #888;
            cursor: not-allowed;
        }
        .error-message {
            background-color: #fee;
            color: #c33;
            padding: 12px 16px;
            border-radius: 8px;
            margin-bottom: 20px;
            font-size: 14px;
            border-left: 4px solid #c33;
            text-align: left;
        }
        .success-message {
            background-color: #d4edda;
            color: #155724;
            padding: 12px 16px;
            border-radius: 8px;
            margin-bottom: 20px;
            font-size: 14px;
            border-left: 4px solid #28a745;
            text-align: left;
        }
        .warning-message {
            background-color: #fff3cd;
            color: #856404;
            padding: 12px 16px;
            border-radius: 8px;
            margin-bottom: 20px;
            font-size: 14px;
            border-left: 4px solid #ffc107;
            text-align: left;
        }
        .field-error {
            color: #e74c3c;
            font-size: 12px;
            margin-top: 5px;
            display: block;
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
        .btn-secondary {
            background-color: #6c757d;
            box-shadow: 0 4px 15px rgba(108,117,125,0.3);
            margin-top: 10px;
        }
        .btn-secondary:hover {
            background-color: #5a6268;
            box-shadow: 0 6px 20px rgba(108,117,125,0.4);
        }
        .divider {
            border: none;
            border-top: 1px solid #eee;
            margin: 25px 0;
        }
        @media (max-width: 600px) {
            .container {
                padding: 40px 25px;
            }
            .logo h1 {
                font-size: 26px;
            }
            h2 {
                font-size: 18px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">
            <h1>NEXO NETWORK</h1>
        </div>

        <h2>Cập Nhật Thông Tin</h2>
        <p class="subtitle">Vui lòng hoàn thiện thông tin tài khoản của bạn</p>

        <#-- Hiển thị message lỗi / thành công -->
        <#if message?has_content>
            <#if message.type == 'success'>
                <div class="success-message">✓ ${message.summary}</div>
            <#elseif message.type == 'error'>
                <div class="error-message">✕ ${message.summary}</div>
            <#elseif message.type == 'warning'>
                <div class="warning-message">⚠ ${message.summary}</div>
            </#if>
        </#if>

        <form id="kc-update-profile-form" action="${url.loginAction}" method="post">

            <#-- Họ -->
            <#if user.editUsernameAllowed!false || (profile.attributesByName["firstName"])??>
            <div class="form-group">
                <label for="firstName">
                    Họ <span class="required">*</span>
                </label>
                <input type="text"
                       id="firstName"
                       name="firstName"
                       value="${(user.firstName)!''}"
                       autofocus
                       autocomplete="given-name"
                       required />
                <#if messagesPerField?? && messagesPerField.existsError('firstName')>
                    <span class="field-error">${messagesPerField.get('firstName')}</span>
                </#if>
            </div>
            </#if>

            <#-- Tên -->
            <#if (profile.attributesByName["lastName"])??>
            <div class="form-group">
                <label for="lastName">
                    Tên <span class="required">*</span>
                </label>
                <input type="text"
                       id="lastName"
                       name="lastName"
                       value="${(user.lastName)!''}"
                       autocomplete="family-name"
                       required />
                <#if messagesPerField?? && messagesPerField.existsError('lastName')>
                    <span class="field-error">${messagesPerField.get('lastName')}</span>
                </#if>
            </div>
            </#if>

            <#-- Email -->
            <#if (profile.attributesByName["email"])??>
            <div class="form-group">
                <label for="email">
                    Email <span class="required">*</span>
                </label>
                <input type="email"
                       id="email"
                       name="email"
                       value="${(user.email)!''}"
                       autocomplete="email"
                       required />
                <#if messagesPerField?? && messagesPerField.existsError('email')>
                    <span class="field-error">${messagesPerField.get('email')}</span>
                </#if>
            </div>
            </#if>

            <#-- Username (nếu cho phép chỉnh) -->
            <#if user.editUsernameAllowed!false>
            <div class="form-group">
                <label for="username">Tên Đăng Nhập</label>
                <input type="text"
                       id="username"
                       name="username"
                       value="${(user.username)!''}"
                       autocomplete="username" />
                <#if messagesPerField?? && messagesPerField.existsError('username')>
                    <span class="field-error">${messagesPerField.get('username')}</span>
                </#if>
            </div>
            </#if>

            <hr class="divider">

            <div class="form-group">
                <#if isAppInitiatedAction??>
                    <button type="submit" id="kc-submit" class="btn" name="login">Lưu Thay Đổi</button>
                    <button type="submit" class="btn btn-secondary" name="cancel-aia" value="true">Hủy Bỏ</button>
                <#else>
                    <button type="submit" id="kc-submit" class="btn" name="login">Lưu Thay Đổi</button>
                </#if>
            </div>

        </form>
    </div>
</body>
</html>
