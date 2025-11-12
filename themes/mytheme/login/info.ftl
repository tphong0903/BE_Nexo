<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NEXO NETWORK - Confirmation</title>
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
        
        <h2>
            <#if messageHeader??>
                ${messageHeader}
            <#else>
                Confirm Your Action
            </#if>
        </h2>
        
        <div class="message">
            <p>Please click the button below to complete your email verification.</p>
        </div>
        
        <#if skipLink??>
            <#-- Không hiển thị link -->
        <#else>
            <#if actionUri?has_content>
                <a href="#" class="btn" onclick="handleConfirm(event, '${actionUri}')">» Confirm Email</a>
            <#elseif pageRedirectUri?has_content>
                <a href="http://localhost:3000/auth/login" class="btn">« Back to Application</a>
            <#elseif client?? && client.baseUrl?has_content>
                <a href="http://localhost:3000/auth/login" class="btn">« Back to Application</a>
            <#else>
                <p class="error">No confirmation link available.</p>
            </#if>
        </#if>
    </div>
    
    <script>
        function handleConfirm(event, actionUrl) {
            event.preventDefault();
            
            const button = event.target;
            button.textContent = 'Verifying...';
            button.style.opacity = '0.6';
            button.style.pointerEvents = 'none';
            
            // Gọi URL xác thực của Keycloak
            fetch(actionUrl, {
                method: 'GET',
                credentials: 'include',
                redirect: 'follow'
            }).then(response => {
                if (response.ok || response.redirected) {
                    // Xác thực thành công, redirect về login
                    window.location.href = 'http://localhost:3000/auth/login';
                } else {
                    throw new Error('Verification failed');
                }
            }).catch(error => {
                console.error('Error:', error);
                button.textContent = 'Verification Failed';
                button.style.backgroundColor = '#dc3545';
                setTimeout(() => {
                    button.textContent = '» Confirm Email';
                    button.style.backgroundColor = '#007bff';
                    button.style.opacity = '1';
                    button.style.pointerEvents = 'auto';
                }, 3000);
            });
        }
    </script>
</body>
</html>